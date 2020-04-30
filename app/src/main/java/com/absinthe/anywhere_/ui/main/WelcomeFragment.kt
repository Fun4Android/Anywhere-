package com.absinthe.anywhere_.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.absinthe.anywhere_.databinding.FragmentWelcomeBinding
import com.absinthe.anywhere_.utils.TimeRecorder
import com.absinthe.anywhere_.viewmodel.AnywhereViewModel

class WelcomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        binding.btnWelcomeStart.setOnClickListener {
            val viewModel = ViewModelProvider(requireActivity()).get(AnywhereViewModel::class.java)
            viewModel.fragment.setValue(InitializeFragment.newInstance())
        }

        TimeRecorder.shouldRecord = false

        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance(): WelcomeFragment {
            return WelcomeFragment()
        }
    }
}