package sery.vlasenko.netsegment.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import sery.vlasenko.netsegment.databinding.FragmentClientBinding

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


}