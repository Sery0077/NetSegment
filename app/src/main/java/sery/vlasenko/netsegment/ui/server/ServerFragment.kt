package sery.vlasenko.netsegment.ui.server

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sery.vlasenko.netsegment.databinding.FragmentServerBinding
import sery.vlasenko.netsegment.ui.server.connections.ConnectionAdapter
import sery.vlasenko.netsegment.ui.server.log.LogAdapter
import sery.vlasenko.netsegment.utils.buildSnackAndShow
import sery.vlasenko.netsegment.utils.orEmpty
import sery.vlasenko.netsegment.utils.showToast
import java.net.*

class ServerFragment : Fragment(), ConnectionAdapter.ClickListener {

    companion object {
        fun newInstance() = ServerFragment()
    }

    private val viewModel: ServerViewModel by viewModels()

    private var _binding: FragmentServerBinding? = null
    private val binding
        get() = _binding!!

    private val logAdapter = LogAdapter()
    private val connAdapter = ConnectionAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentServerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        setClickers()

        viewModel.ipState.observe(viewLifecycleOwner) {
            handleIpState(it)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connRecyclerState.collectLatest {
                    when (it) {
                        is ConnRecyclerState.ConnAdd -> {
                            connAdapter.notifyItemInserted(it.position)
                        }
                        is ConnRecyclerState.ConnRemove -> {
                            connAdapter.notifyItemRemoved(it.position)
                        }
                        is ConnRecyclerState.LogAdd -> {
                            connAdapter.notifyItemChanged(it.position, it.logItem)
                            logAdapter.notifyItemInserted(it.position)
                        }
                        is ConnRecyclerState.ConnChanged -> {
                            connAdapter.notifyItemChanged(it.position, it.connectionItem)
                        }
                        is ConnRecyclerState.ConnClear -> {
                            connAdapter.notifyItemRangeRemoved(0, connAdapter.itemCount)
                        }
                    }
                }
            }
        }

        viewModel.singleEvent.observe(viewLifecycleOwner) {
            when (it) {
                is SingleEvent.ShowToastEvent -> {
                    showToast(it.msg)
                }
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            handleUiState(it)
        }
    }

    private fun handleUiState(uiState: ServerButtonState) {
        when (uiState) {
            ServerButtonState.TcpSocketOpened -> {
                binding.run {
                    etTcpPort.isEnabled = false
                    btnOpenTcpSocket.isEnabled = false
                    btnCloseTcpSocket.isEnabled = true
                }
            }
            ServerButtonState.TcpSocketClosed -> {
                binding.run {
                    etTcpPort.isEnabled = true
                    btnOpenTcpSocket.isEnabled = true
                    btnCloseTcpSocket.isEnabled = false
                }
            }
            ServerButtonState.UdpSocketOpened -> {
                binding.run {
                    etUdpPort.isEnabled = false
                    btnOpenUdpSocket.isEnabled = false
                    btnCloseUdpSocket.isEnabled = true
                }
            }
            ServerButtonState.UdpSocketClosed -> {
                binding.run {
                    etUdpPort.isEnabled = true
                    btnOpenUdpSocket.isEnabled = true
                    btnCloseUdpSocket.isEnabled = false
                }
            }
        }
    }

    private fun setClickers() {
        binding.btnOpenTcpSocket.setOnClickListener {
            viewModel.onOpenTcpSocketClicked(binding.etTcpPort.text.toString())
        }

        binding.btnCloseTcpSocket.setOnClickListener {
            viewModel.onCloseTcpSocketClicked()
        }

        binding.btnOpenUdpSocket.setOnClickListener {
            viewModel.onOpenUdpSocketClicked(binding.etUdpPort.text.toString())
        }

        binding.btnCloseUdpSocket.setOnClickListener {
            viewModel.onCloseUdpSocketClicked()
        }
    }


    private fun initView() {
        binding.rvConnections.adapter = connAdapter
        binding.rvConnections.setHasFixedSize(true)
        connAdapter.submitList(viewModel.connectionItems)

        binding.tvLocalIp.text = getLocalIp()
    }

    private fun getLocalIp(): String {
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { return it.hostAddress.orEmpty() }
        }
        return ""
    }

    private fun handleIpState(state: ServerUiState) {
        when (state) {
            is ServerUiState.Error -> {
                buildSnackAndShow(binding.root) {
                    viewModel.getIp()
                }
                showToast(state.message.toString())
            }
            is ServerUiState.Loaded -> {
                binding.tvIp.text = state.data
            }
            ServerUiState.Loading -> {

            }
        }
    }

    override fun onStartTestClick(pos: Int) {
        viewModel.onStartTestClick(pos)
    }

    override fun onStopTestClick(pos: Int) {
        viewModel.onStopTestClick(pos)
    }

    override fun onResultClick(pos: Int) {
        viewModel.onResultClick(pos)
    }
}