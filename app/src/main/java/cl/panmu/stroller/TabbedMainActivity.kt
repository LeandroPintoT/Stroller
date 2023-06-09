package cl.panmu.stroller

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import cl.panmu.stroller.databinding.ActivityTabbedMainBinding
import cl.panmu.stroller.ui.fragments.AudioFragment
import cl.panmu.stroller.ui.fragments.ConexionFragment
import cl.panmu.stroller.ui.fragments.EscenasFragment
import cl.panmu.stroller.ui.main.PageViewModel
import cl.panmu.stroller.ui.main.PagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.time.LocalDateTime
import java.time.ZoneOffset

class TabbedMainActivity : AppCompatActivity() {

    //lateinit var mainHandler: Handler
    private lateinit var binding: ActivityTabbedMainBinding
    private val viewModel: PageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTabbedMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pagerAdapter = PagerAdapter(supportFragmentManager, lifecycle)
        val viewPager: ViewPager2 = binding.viewPager
        pagerAdapter.addFragment(ConexionFragment(), resources.getString(R.string.fragment_title_conexion))
        pagerAdapter.addFragment(EscenasFragment(), resources.getString(R.string.fragment_title_escenas))
        pagerAdapter.addFragment(AudioFragment(), resources.getString(R.string.fragment_title_audio))
        viewPager.adapter = pagerAdapter

        val tabs: TabLayout = binding.tabs
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = pagerAdapter.getFragmentTitle(position)
        }.attach()

        viewModel.isConnected.observe(this) { isConnected ->
            runOnUiThread {
                val btnsLayout: TableLayout = findViewById(R.id.tableLayoutBtnsStream)
                btnsLayout.visibility = if (isConnected) View.VISIBLE else View.INVISIBLE
            }
        }

        viewModel.isStreaming.observe(this) { isStreaming ->
            // actualiza boton stream
            runOnUiThread {
                val btnStream: Button = findViewById(R.id.btnStream)
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
            runOnUiThread {
                val btnRecord: Button = findViewById(R.id.btnRecord)
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