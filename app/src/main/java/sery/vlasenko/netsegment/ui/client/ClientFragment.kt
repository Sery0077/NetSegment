package sery.vlasenko.netsegment.ui.client

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sery.vlasenko.netsegment.databinding.FragmentClientBinding
import sery.vlasenko.netsegment.utils.showToast
import sery.vlasenko.netsegment.utils.toBytes
import sery.vlasenko.netsegment.utils.toLong
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Calendar

class ClientFragment : Fragment() {

    companion object {
        fun newInstance() = ClientFragment()
    }

    private val viewModel: ClientViewModel by viewModels()

    private var _binding: FragmentClientBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartTest.setOnClickListener {
            val ip = binding.etServerIp.text.toString()
            val port = binding.etPort.text.toString().toInt()

            Thread {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port))

                if (socket.isConnected) {
                    Handler(Looper.getMainLooper()).post {
                        showToast("Connected")
                    }
                }

                lifecycleScope.launch {
                    repeat(5) {
                        socket.getOutputStream()
                            .write(Calendar.getInstance().timeInMillis.toBytes())
                        delay(2000)
                    }
                }

                val data = ByteArray(Long.SIZE_BYTES)
                while (true) {
                    val count: Int = socket.getInputStream().read(data, 0, Long.SIZE_BYTES)
                    if (count > 0) {
                        println("responsed ${data.toLong()}")
                    }
                }
            }.start()
        }
    }

}