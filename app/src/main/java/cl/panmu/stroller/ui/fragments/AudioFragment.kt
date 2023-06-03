package cl.panmu.stroller.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cl.panmu.stroller.R
import cl.panmu.stroller.ui.main.PageViewModel
import cl.panmu.stroller.util.ObsMath

class AudioFragment : Fragment() {

    private val maxProgress: Float = 200f
    private var _binding: View? = null
    private lateinit var view: View
    private val viewModel: PageViewModel by activityViewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = inflater.inflate(R.layout.fragment_audio, container, false)
        view = _binding!!

        val textoTitulo: TextView = view.findViewById(R.id.txtTitulo)
        val btnRecargar: ImageButton = view.findViewById(R.id.btnRecargar)

        btnRecargar.setOnClickListener {
            obtenerAudio()
        }

        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            textoTitulo.text = resources.getText(if (isConnected) R.string.titulo_audio else R.string.titulo_no_conectado)

            if (isConnected) {
                btnRecargar.visibility = View.VISIBLE
                recargarAudio()
            }
            else {
                btnRecargar.visibility = View.INVISIBLE
                val audioLayout: TableLayout = view.findViewById(R.id.audioLayout)
                audioLayout.removeAllViews()
            }
        }

        viewModel.audioList.observe(viewLifecycleOwner) {
            recargarAudio()
        }

        return binding
    }

    private fun obtenerAudio() {
        viewModel.clearAudioList()
        viewModel.obsController.value?.getCurrentProgramScene { resCurrScene ->
            val currScene = resCurrScene.currentProgramSceneName
            viewModel.obsController.value?.getSceneItemList(currScene) { resItemList ->
                for (item in resItemList.sceneItems) {
                    if (item.sourceType == "OBS_SOURCE_TYPE_SCENE") {
                        viewModel.obsController.value?.getGroupSceneItemList(item.sourceName) { res2 ->
                            for (item2 in res2.sceneItems) {
                                if (item2.sourceType == "OBS_SOURCE_TYPE_INPUT") {
                                    viewModel.obsController.value?.getInputVolume(item2.sourceName) { resInput ->
                                        if (resInput.isSuccessful) {
                                            requireActivity().runOnUiThread { viewModel.addToAudioList(item2.sourceName, resInput.inputVolumeMul) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else if (item.sourceType == "OBS_SOURCE_TYPE_INPUT") {
                        viewModel.obsController.value?.getInputVolume(item.sourceName) { resInput ->
                            if (resInput.isSuccessful) {
                                requireActivity().runOnUiThread { viewModel.addToAudioList(item.sourceName, resInput.inputVolumeMul) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun recargarAudio() {
        val tabla: TableLayout = view.findViewById(R.id.audioLayout)
        tabla.removeAllViews()

        for (audio in if (viewModel.audioList.value != null) viewModel.audioList.value!!.sortedBy { audio -> audio.escena } else ArrayList()) {
            val row = TableRow(view.context)
            row.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            tabla.addView(row)

            val linear = LinearLayout(view.context)
            linear.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            linear.orientation = LinearLayout.VERTICAL
            row.addView(linear)

            val texto = TextView(view.context)
            texto.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            texto.text = audio.escena
            texto.textSize = resources.getDimension(R.dimen.audio_text)
            texto.setPadding(resources.getDimension(R.dimen.audio_text_padding).toInt(), 0, resources.getDimension(R.dimen.audio_text_padding).toInt(), 0)
            texto.setTextColor(resources.getColor(R.color.white, null))
            linear.addView(texto)

            val seek = SeekBar(view.context)
            seek.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            seek.progressTintList = ColorStateList.valueOf(resources.getColor(R.color.ocean_blue, null))
            seek.thumbTintList = ColorStateList.valueOf(resources.getColor(R.color.ocean_blue, null))
            seek.max = maxProgress.toInt()
            //seek.progress = (audio.volumenMul.toFloat() * maxProgress).roundToInt()
            seek.progress = ObsMath.getProgressFromMul(audio.volumenMul, maxProgress)
            seek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekbar: SeekBar, progress: Int, fromUser: Boolean) {
                    //viewModel.obsController.value?.setInputVolume(audio.escena, (progress.toFloat() / maxProgress), null, 300)
                    viewModel.obsController.value?.setInputVolume(audio.escena, ObsMath.getMulFromProgress(progress, maxProgress), null, 300)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    Log.d("STARTTRACK", "StartTracking")
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    Log.d("STOPTRACK", "StopTracking")
                }
            })
            linear.addView(seek)

            val imgbtn = ImageButton(view.context)
            imgbtn.layoutParams = TableRow.LayoutParams(resources.getDimension(R.dimen.audio_imgbtn).toInt(), resources.getDimension(R.dimen.audio_imgbtn).toInt())
            imgbtn.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.dark_ocean_blue, null))
            imgbtn.adjustViewBounds = true
            imgbtn.scaleType = ImageView.ScaleType.FIT_CENTER
            imgbtn.setPadding(1)
            imgbtn.setImageResource(R.drawable.ic_volume_on)
            imgbtn.contentDescription = resources.getText(R.string.desc_audio_imgbtn)
            imgbtn.setOnClickListener {
                viewModel.obsController.value?.toggleInputMute(audio.escena) {
                    imgbtn.setImageResource(if (it.inputMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on)
                }
            }
            row.addView(imgbtn)
        }
    }
}