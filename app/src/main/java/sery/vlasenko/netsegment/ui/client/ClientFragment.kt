package sery.vlasenko.netsegment.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.ui.server.ServerUiState
import sery.vlasenko.netsegment.ui.server.SingleEvent
import sery.vlasenko.netsegment.ui.server.log.LogAdapter
import sery.vlasenko.netsegment.ui.server.log.LogState
import sery.vlasenko.netsegment.utils.buildSnackAndShow
import sery.vlasenko.netsegment.utils.showToast
import sery.vlasenko.netsegment.databinding.FragmentClientBinding
import sery.vlasenko.netsegment.model.connections.Protocol
import kotlin.math.log

class ClientFragment : Fragment() {

    companion object {
        fun newInstance() = ClientFragment()
    }

    private val viewModel: ClientViewModel by viewModels()

    private var _binding: FragmentClientBinding? = null
    private val binding
        get() = _binding!!

    private val logAdapter = LogAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        setClickers()

        viewModel.ipState.observe(viewLifecycleOwner) {
            handleIpState(it)
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            handleUiState(it)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logState.collectLatest {
                    when (it) {
                        is LogState.LogAdd -> {
                            binding.rvLog.recycledViewPool.clear()
                            logAdapter.notifyItemInserted(it.position)
                            binding.rvLog.scrollToPosition(it.position)
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
                is SingleEvent.ConnEvent.PingGet -> {
                    binding.tvPing.text = getString(R.string.ping_pattern, it.ping)
                }
                is SingleEvent.ConnEvent.TestStart -> {

                }
            }
        }
    }

    private fun handleUiState(state: ClientUiState) {
        when (state) {
            ClientUiState.Connected -> {
                binding.btnConnect.isEnabled = false
                binding.btnDisconnect.isEnabled = true
            }
            ClientUiState.Disconnected -> {
                binding.etServerIp.isEnabled = true
                binding.etPort.isEnabled = true

                binding.btnConnect.isEnabled = true
                binding.btnDisconnect.isEnabled = false
            }
            ClientUiState.Connecting -> {
                binding.etServerIp.isEnabled = false
                binding.etPort.isEnabled = false

                binding.btnConnect.isEnabled = false
                binding.btnDisconnect.isEnabled = false
            }
        }
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

    private fun initViews() {
        binding.rvLog.layoutManager = object: LinearLayoutManager(requireContext()) {
            override fun supportsPredictiveItemAnimations(): Boolean {
                return false
            }
        }
        binding.rvLog.adapter = logAdapter
        binding.rvLog.setHasFixedSize(true)

        logAdapter.submitList(viewModel.logs)
    }

    private fun setClickers() {
        binding.btnConnect.setOnClickListener {
            val ip = binding.etServerIp.text.toString()
            val port = binding.etPort.text.toString()
            val protocol = handleProtocol()

            viewModel.onConnectClicked(ip, port, protocol)
        }

        binding.btnDisconnect.setOnClickListener {
            viewModel.onDisconnectClicked()
        }
    }

    private fun handleProtocol(): Protocol {
        return when (binding.rgProtocol.checkedRadioButtonId) {
            binding.rbTcp.id -> Protocol.TCP
            binding.rbUdp.id -> Protocol.UDP
            else -> throw IllegalArgumentException("Unknown id ${binding.rgProtocol.checkedRadioButtonId}")
        }
    }

}