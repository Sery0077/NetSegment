package sery.vlasenko.netsegment.ui.server

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import sery.vlasenko.netsegment.R

class ServerFragment : Fragment() {

    companion object {
        fun newInstance() = ServerFragment()
    }

    private lateinit var viewModel: ServerViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_server, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ServerViewModel::class.java)

    }

}