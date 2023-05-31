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
import sery.vlasenko.netsegment.model.connections.ConnectionState
import sery.vlasenko.netsegment.model.connections.Protocol
import sery.vlasenko.netsegment.ui.server.connections.ConnectionItem
import sery.vlasenko.netsegment.ui.server.dialog_start_test.DialogStartTest
import sery.vlasenko.netsegment.ui.server.log.LogAdapter
import sery.vlasenko.netsegment.utils.*
import sery.vlasenko.netsegment.utils.extensions.setConnState
import java.net.Inet4Address
import java.net.NetworkInterface

class ServerFragment : Fragment(), DialogStartTest.DialogStartTestClickListener {

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
                    binding.connTvPing.text = getString(R.string.ping_pattern, it.ping)
                }
                is SingleEvent.ConnEvent.AddLog -> {
                    logAdapter.notifyItemInserted(it.pos)
                }
                SingleEvent.ConnState.ConnIdle -> {}
                SingleEvent.ConnState.ConnMeasure -> {}
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

        with(binding) {
            connTvIp.text = conn.ip
            connTvPort.text = conn.port

            connBtnShowResult.isEnabled = conn.isResultAvailable

            when (conn.state) {
                ConnectionState.IDLE -> {
                    connBtnStartTest.isEnabled = true
                    connBtnStopTest.isEnabled = false

                    binding.connState.setConnState(ConnectionState.IDLE)
                }
                ConnectionState.MEASURE -> {
                    connBtnStartTest.isEnabled = false
                    connBtnStopTest.isEnabled = true

                    binding.connState.setConnState(ConnectionState.MEASURE)
                }
            }

            setProtocol(conn.protocol)

            connection.isVisible = true
        }
    }

    private fun setProtocol(protocol: Protocol) {
        with(binding.connTvProtocol) {
            when (protocol) {
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
            text = protocol.name
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
        binding.btnOpen.throttleClick {
            val port = binding.etPort.text.toString()
            val protocol = handleProtocol()

            viewModel.onOpenSocketClicked(port, protocol)
        }

        binding.btnClose.throttleClick {
            viewModel.onCloseSocketClicked()
        }

        binding.connBtnStartTest.throttleClick {
            showDialogStartTest()
        }

        binding.connBtnStopTest.throttleClick {
            viewModel.onStopTestClick()
        }

        binding.connBtnShowResult.throttleClick {
            viewModel.onResultClick()
        }
    }

    private fun showDialogStartTest() {
        DialogStartTest().show(childFragmentManager, null)
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

        binding.rgProtocol.setOnCheckedChangeListener { _, checkedId ->
            binding.etPort.setText(
                when (checkedId) {
                    binding.rbTcp.id -> {
                        PortHelper.TCP_PORT.toString()
                    }
                    binding.rbUdp.id -> {
                        PortHelper.UDP_PORT.toString()
                    }
                    else -> throw IllegalStateException("No port for $checkedId")
                }
            )
        }

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

    override fun onStartTestClick(iterationCount: Int) {
        viewModel.onStartTestClick(iterationCount)
    }
}