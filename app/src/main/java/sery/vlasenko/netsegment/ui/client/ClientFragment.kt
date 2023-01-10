package sery.vlasenko.netsegment.ui.client

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import sery.vlasenko.netsegment.R

class ClientFragment : Fragment() {

    companion object {
        fun newInstance() = ClientFragment()
    }

    private lateinit var viewModel: ClientViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_client, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this)[ClientViewModel::class.java]

    }

}