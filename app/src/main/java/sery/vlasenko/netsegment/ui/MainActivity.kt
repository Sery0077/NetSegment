package sery.vlasenko.netsegment.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.databinding.ActivityMainBinding
import sery.vlasenko.netsegment.ui.client.ClientFragment
import sery.vlasenko.netsegment.ui.server.ServerFragment

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var binding: ActivityMainBinding

    private lateinit var serverFragment: Fragment
    private lateinit var clientFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFragmentContainer()
        setClickers()
    }

    private fun initFragmentContainer() {
        serverFragment = ServerFragment.newInstance()
        clientFragment = ClientFragment.newInstance()
    }

    private fun setClickers() {
        binding.radioServerClient.setOnCheckedChangeListener { _, i ->
            if (i == R.id.rb_client) {
                supportFragmentManager.beginTransaction()
                    .replace(binding.fragmentContainerViewTag.id, clientFragment)
                    .commitAllowingStateLoss()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(binding.fragmentContainerViewTag.id, serverFragment)
                    .commitAllowingStateLoss()
            }
        }

        binding.radioServerClient.check(R.id.rb_server)
    }

}