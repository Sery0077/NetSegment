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
                viewModel.recyclerState.collectLatest {
                    when (it) {
                        is RecyclerState.ConnAdd -> {
                            connAdapter.notifyItemInserted(it.position)
                        }
                        is RecyclerState.ConnRemove -> {
                            connAdapter.notifyItemRemoved(it.position)
                        }
                        is RecyclerState.LogAdd -> {
                            connAdapter.notifyItemChanged(it.position, it.logItem)
                            logAdapter.notifyItemInserted(it.position)
                        }
                        is RecyclerState.ConnChanged -> {
                            connAdapter.notifyItemChanged(it.position, it.ping)
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

    private fun handleUiState(uiState: UiState) {
        when (uiState) {
            UiState.SocketClosed -> {
                binding.run {
                    etPort.isEnabled = true
                    btnOpenSocket.isEnabled = true
                    btnCloseSocket.isEnabled = false
                }
            }
            UiState.SocketOpened -> {
                binding.run {
                    etPort.isEnabled = false
                    btnOpenSocket.isEnabled = false
                    btnCloseSocket.isEnabled = true
                }
            }
        }
    }

    private fun setClickers() {
        binding.btnOpenSocket.setOnClickListener {
            viewModel.onOpenSocketClicked(binding.etPort.text.toString())
        }

        binding.btnCloseSocket.setOnClickListener {
            viewModel.onCloseSocketClicked()
        }
    }


    private fun initView() {
        binding.rvConnections.adapter = connAdapter
        binding.rvConnections.setHasFixedSize(true)
        connAdapter.submitList(viewModel.connections)

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
}