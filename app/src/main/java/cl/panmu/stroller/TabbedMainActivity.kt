package cl.panmu.stroller

import android.os.Bundle
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

class TabbedMainActivity : AppCompatActivity() {

    //lateinit var mainHandler: Handler
    private lateinit var binding: ActivityTabbedMainBinding
    private val viewModel: PageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTabbedMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //mainHandler = Handler(Looper.getMainLooper())

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
    }

    /*private val updateTextTask = object : Runnable {
        override fun run() {
            mainHandler.postDelayed(this, 3000)
            if (viewModel.isConnected.value != null && viewModel.isConnected.value!!) {
                viewModel.obsController.value?.getStreamStatus { streamStats ->
                    runOnUiThread {
                        val dif = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC).toInstant().toEpochMilli() - streamStats.outputDuration.toLong()
                        viewModel.timeStream(Instant.ofEpochMilli(dif).atZone(ZoneOffset.UTC).toLocalDateTime())
                        viewModel.isStreaming(streamStats.outputActive)
                        Log.d("TABMAINSTREAM", "NOW: ${LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()} - " +
                                "STREAM - ${streamStats.outputDuration} - DIFF: $dif")
                    }
                }
                viewModel.obsController.value?.getRecordStatus { recordStats ->
                    runOnUiThread {
                        val dif = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC).toInstant().toEpochMilli() - recordStats.outputDuration.toLong()
                        val ldt = Instant.ofEpochMilli(dif).atZone(ZoneOffset.UTC).toLocalDateTime()
                        viewModel.timeRecord(ldt)
                        viewModel.isRecording(recordStats.outputActive)
                        Log.d("TABMAINSTREAM", "NOW: ${LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()} - " +
                                "STREAM - ${recordStats.outputDuration} - DIFF: $dif")
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        //mainHandler.removeCallbacks(updateTextTask)
    }

    override fun onResume() {
        super.onResume()
        //mainHandler.post(updateTextTask)
    }*/
}