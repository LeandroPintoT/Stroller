package cl.panmu.stroller

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import cl.panmu.stroller.databinding.ActivityTabbedMainBinding
import cl.panmu.stroller.ui.fragments.AudioFragment
import cl.panmu.stroller.ui.fragments.ChatFragment
import cl.panmu.stroller.ui.fragments.EscenasFragment
import cl.panmu.stroller.ui.fragments.EstadoFragment
import cl.panmu.stroller.ui.fragments.FuentesFragment
import cl.panmu.stroller.ui.fragments.UtilidadesFragment
import cl.panmu.stroller.ui.main.PageViewModel
import cl.panmu.stroller.ui.main.PagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset

class TabbedMainActivity : AppCompatActivity() {

    //lateinit var mainHandler: Handler
    private lateinit var binding: ActivityTabbedMainBinding
    private val viewModel: PageViewModel by viewModels()
    private var alto: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTabbedMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pagerAdapter = PagerAdapter(supportFragmentManager, lifecycle)
        val viewPager: ViewPager2 = binding.viewPager
        viewPager.offscreenPageLimit = 4
        pagerAdapter.addFragment(EstadoFragment(), resources.getString(R.string.fragment_title_estado))
        pagerAdapter.addFragment(EscenasFragment(), resources.getString(R.string.fragment_title_escenas))
        pagerAdapter.addFragment(UtilidadesFragment(), resources.getString(R.string.fragment_title_utilidades))
        pagerAdapter.addFragment(FuentesFragment(), resources.getString(R.string.fragment_title_fuentes))
        pagerAdapter.addFragment(AudioFragment(), resources.getString(R.string.fragment_title_audio))
        pagerAdapter.addFragment(ChatFragment(), resources.getString(R.string.fragment_title_chat))
        viewPager.adapter = pagerAdapter

        val tabs: TabLayout = binding.tabs
        tabs.addOnTabSelectedListener(TabSelectedListener(this, viewModel))
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = pagerAdapter.getFragmentTitle(position)
        }.attach()

        val userlogged = File(filesDir, "userlogged.cfg")
        if (userlogged.exists() && userlogged.readLines().isNotEmpty()) {
            val jsonObs = JSONObject(userlogged.readLines().first())
            viewModel.username(jsonObs.get("user").toString())
        }

        val btnToggleReplay = findViewById<Button>(R.id.btnToggleReplay)
        val btnGetReplay = findViewById<Button>(R.id.btnGetReplay)
        btnToggleReplay.setOnClickListener {
            viewModel.obsController.value?.toggleReplayBuffer {
                if (!it.isSuccessful)
                    runOnUiThread {
                        btnToggleReplay.text = resources.getString(R.string.btn_iniciar_buffer)
                        btnGetReplay.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.dark_ocean_blue, null))
                        btnGetReplay.isEnabled = false
                        AlertDialog
                            .Builder(this)
                            .setTitle(R.string.alerta_titulo_buffer)
                            .setMessage(R.string.alerta_msg_buffer)
                            .setPositiveButton(R.string.alerta_btn_pos_conexion_error) { _, _ -> }
                            .show()
                    }
            }
        }
        btnGetReplay.setOnClickListener {
            viewModel.obsController.value?.getReplayBufferStatus {
                if (it.isSuccessful && it.outputActive) {
                    viewModel.obsController.value?.saveReplayBuffer { saveRes ->
                        if (saveRes.isSuccessful) {
                            viewModel.obsController.value?.getLastReplayBufferReplay {res ->
                                if (res.isSuccessful)
                                    runOnUiThread { Toast.makeText(this, resources.getString(R.string.toast_msg_pos_guardar_clip), Toast.LENGTH_SHORT).show() }
                            }
                        }
                    }
                }
                else {
                    runOnUiThread { Toast.makeText(this, resources.getString(R.string.toast_msg_neg_guardar_clip), Toast.LENGTH_SHORT).show() }
                }
            }
        }

        viewModel.isConnected.observe(this) { isConnected ->
            val btnsLayout: TableLayout = findViewById(R.id.tableLayoutBtnsStream)
            runOnUiThread {
                if (isConnected) {
                    btnsLayout.visibility = View.VISIBLE
                    btnsLayout.layoutParams.height = alto
                }
                else {
                    alto = btnsLayout.layoutParams.height
                    btnsLayout.visibility = View.INVISIBLE
                    btnsLayout.layoutParams.height = 1
                }
            }
            viewModel.obsController.value?.getReplayBufferStatus {
                if (it.isSuccessful) {
                    btnToggleReplay.text = resources.getString(if(it.outputActive) R.string.btn_detener_buffer else R.string.btn_iniciar_buffer)
                    btnGetReplay.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.ocean_blue, null))
                    btnGetReplay.isEnabled = true
                }
                else {
                    btnToggleReplay.text = resources.getString(R.string.btn_iniciar_buffer)
                    btnGetReplay.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.dark_ocean_blue, null))
                    btnGetReplay.isEnabled = false
                }
            }
        }

        viewModel.isStreaming.observe(this) { isStreaming ->
            val btnStream: Button = findViewById(R.id.btnStream)
            // actualiza boton stream
            runOnUiThread {
                btnStream.setText(if (isStreaming) R.string.btn_detener_transmision else R.string.btn_iniciar_transmision)
                // Setea la funcion usada dependiendo del estado de la transmision
                btnStream.setOnClickListener(
                    if (isStreaming) {
                        {
                            AlertDialog
                                .Builder(this)
                                .setTitle(R.string.alerta_titulo_emision)
                                .setMessage(R.string.alerta_msg_emision_final)
                                .setPositiveButton(R.string.alerta_btn_pos_emision_final) { _, _ ->
                                    viewModel.obsController.value?.stopStream {
                                        if (it.isSuccessful)
                                            runOnUiThread { viewModel.isStreaming(false) }
                                        else
                                            Toast.makeText(this, resources.getText(R.string.toast_msg_neg_detener_emision), Toast.LENGTH_LONG).show()
                                    }
                                }
                                .setNegativeButton(R.string.alerta_btn_neg_emision_final) { _, _ -> }
                                .show()
                        }
                    } else {
                        {
                            AlertDialog
                                .Builder(this)
                                .setTitle(R.string.alerta_titulo_emision)
                                .setMessage(R.string.alerta_msg_emision_inicio)
                                .setPositiveButton(R.string.alerta_btn_pos_emision_inicio) { _, _ ->
                                    viewModel.obsController.value?.startStream {
                                        if (it.isSuccessful) {
                                            runOnUiThread {
                                                viewModel.isStreaming(true)
                                                viewModel.timeStream(LocalDateTime.now(ZoneOffset.UTC))
                                            }
                                        } else
                                            Toast.makeText(this, resources.getText(R.string.toast_msg_neg_iniciar_emision), Toast.LENGTH_LONG).show()
                                    }
                                }
                                .setNegativeButton(R.string.alerta_btn_neg_emision_inicio) { _, _ -> }
                                .show()
                        }
                    }
                )
            }
        }

        viewModel.isRecording.observe(this) { isRecording ->
            // actualiza boton record
            val btnRecord: Button = findViewById(R.id.btnRecord)
            runOnUiThread {
                btnRecord.setText(if (isRecording) R.string.btn_detener_grabacion else R.string.btn_iniciar_grabacion)
                // si esta desconectado, hace el startForResult, si no, desconecta y recarga
                btnRecord.setOnClickListener(
                    if (isRecording) { {
                        AlertDialog
                            .Builder(this)
                            .setTitle(R.string.alerta_titulo_grabacion)
                            .setMessage(R.string.alerta_msg_grabacion_final)
                            .setPositiveButton(R.string.alerta_btn_pos_grabacion_final) { _, _ ->
                                viewModel.obsController.value?.stopRecord {
                                    if (it.isSuccessful)
                                        runOnUiThread { viewModel.isRecording(false) }
                                    else
                                        Toast.makeText(this, resources.getText(R.string.toast_msg_neg_detener_grabacion), Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton(R.string.alerta_btn_neg_grabacion_final) { _, _ -> }
                            .show()
                    } }
                    else { {
                        AlertDialog
                            .Builder(this)
                            .setTitle(R.string.alerta_titulo_grabacion)
                            .setMessage(R.string.alerta_msg_grabacion_inicio)
                            .setPositiveButton(R.string.alerta_btn_pos_grabacion_inicio) { _, _ ->
                                viewModel.obsController.value?.startRecord {
                                    if (it.isSuccessful) {
                                        runOnUiThread {
                                            viewModel.isRecording(true)
                                            viewModel.timeRecord(LocalDateTime.now(ZoneOffset.UTC))
                                        }
                                    }
                                    else
                                        Toast.makeText(this, resources.getText(R.string.toast_msg_neg_iniciar_grabacion), Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton(R.string.alerta_btn_neg_grabacion_inicio) { _, _ -> }
                            .show()
                    } }
                )
            }
        }
    }
}

class TabSelectedListener(private val ctx: TabbedMainActivity, private val viewModel: PageViewModel) : TabLayout.OnTabSelectedListener {
    private var alto: Int = 0

    override fun onTabSelected(tab: TabLayout.Tab?) {
        if (tab?.text == "Chat") {
            ctx.runOnUiThread {
                val btnsLayout: TableLayout = ctx.findViewById(R.id.tableLayoutBtnsStream)
                btnsLayout.visibility = View.INVISIBLE
                alto = btnsLayout.layoutParams.height
                btnsLayout.layoutParams.height = 1
            }
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        if (tab?.text == "Chat") {
            if (viewModel.isConnected.value != null && viewModel.isConnected.value!!) {
                val btnsLayout: TableLayout = ctx.findViewById(R.id.tableLayoutBtnsStream)
                ctx.runOnUiThread {
                    btnsLayout.visibility = View.VISIBLE
                    btnsLayout.layoutParams.height = alto
                }
            }
        }
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
        Log.d("TABSEL", "onTabReselected: " + tab?.text)
    }
}