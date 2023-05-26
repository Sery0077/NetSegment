package sery.vlasenko.netsegment.ui.server

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.databinding.FragmentServerBinding
import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.connections.ConnectionState
import sery.vlasenko.netsegment.model.connections.Protocol
import sery.vlasenko.netsegment.ui.server.connections.ConnectionAdapter
import sery.vlasenko.netsegment.ui.server.connections.ConnectionItem
import sery.vlasenko.netsegment.ui.server.connections.ConnectionItemState
import sery.vlasenko.netsegment.ui.server.log.LogAdapter
import sery.vlasenko.netsegment.utils.*
import java.net.Inet4Address
import java.net.NetworkInterface

class ServerFragment : Fragment(), ConnectionAdapter.ClickListener {

    companion object {
        fun newInstance() = ServerFragment()
    }

    private val viewModel: ServerViewModel by viewModels()

    private var _binding: FragmentServerBinding? = null
    private val binding
        get() = _binding!!

    private val logAdapter = LogAdapter()

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

        viewModel.connItem.observe(viewLifecycleOwner) { conn ->
            handleConnection(conn)
        }

        viewModel.singleEvent.observe(viewLifecycleOwner) {
            when (it) {
                is SingleEvent.ShowToastEvent -> {
                    showToast(it.msg)
                }
                is SingleEvent.ConnEvent.PingGet -> {
                    binding.connTvPing.text = getString(R.string.ping_pattern, it.ping.toString())
                }
                SingleEvent.ConnEvent.TestStart -> {
                    binding.connBtnStartTest.isEnabled = false
                    binding.connBtnStopTest.isEnabled = true
                }
                SingleEvent.ConnEvent.TestEnd -> {
                    binding.connBtnStartTest.isEnabled = true
                    binding.connBtnStopTest.isEnabled = false
                }
                is SingleEvent.ConnEvent.AddLog -> {
                    logAdapter.notifyItemInserted(it.pos)
                }
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            handleUiState(it)
        }
    }

    private fun handleConnection(conn: ConnectionItem?) {
        if (conn == null) {
            binding.connection.visibility = View.GONE
            return
        }

        conn.let {
            with(binding) {
                connTvIp.text = conn.ip
                connTvPort.text = conn.port.toString()

                when (conn.state) {
                    ConnectionItemState.IDLE -> {
                        connBtnStartTest.isEnabled = true
                        connBtnStopTest.isEnabled = false
                    }
                    ConnectionItemState.TESTING -> {
                        connBtnStartTest.isEnabled = false
                        connBtnStopTest.isEnabled = true
                    }
                }

                with(connTvProtocol) {
                    when (conn.protocol) {
                        Protocol.UDP -> setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.teal_700
                            )
                        )
                        Protocol.TCP -> setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.purple_700
                            )
                        )
                    }
                    text = conn.protocol.name
                }

                connection.isVisible = true
            }
        }
    }

    private fun handleUiState(uiState: ServerButtonState) {
        when (uiState) {
            ServerButtonState.SocketOpened -> {
                binding.run {
                    etPort.isEnabled = false
                    rgProtocol.disable()

                    btnOpen.isEnabled = false
                    btnClose.isEnabled = true
                }
            }
            ServerButtonState.SocketClosed -> {
                binding.run {
                    etPort.isEnabled = true
                    rgProtocol.enable()

                    btnOpen.isEnabled = true
                    btnClose.isEnabled = false

                    binding.connection.visibility = View.GONE
                }
            }
        }
    }

    private fun setClickers() {
        binding.btnOpen.setOnClickListener {
            val port = binding.etPort.text.toString()
            val protocol = handleProtocol()

            viewModel.onOpenSocketClicked(port, protocol)
        }

        binding.btnClose.setOnClickListener {
            viewModel.onCloseSocketClicked()
        }
    }

    private fun handleProtocol(): Protocol {
        return when (binding.rgProtocol.checkedRadioButtonId) {
            binding.rbTcp.id -> Protocol.TCP
            binding.rbUdp.id -> Protocol.UDP
            else -> throw IllegalArgumentException("Unknown id ${binding.rgProtocol.checkedRadioButtonId}")
        }
    }


    private fun initView() {
        binding.tvLocalIp.text = getLocalIp()

        logAdapter.submitList(viewModel.logs)
        binding.connRvLogs.adapter = logAdapter
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