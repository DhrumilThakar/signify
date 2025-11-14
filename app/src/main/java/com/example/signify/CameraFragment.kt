package com.example.signify

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.signify.databinding.FragmentCameraBinding

class CameraFragment : Fragment() {

    private lateinit var binding: FragmentCameraBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainActivity

        binding.cameraFragment.setOnTouchListener(object : OnSwipeTouchListener(context) {
            override fun onSwipeUp() {
                super.onSwipeUp()
                val intent = Intent()
                intent.type = "video/*"
                intent.action = Intent.ACTION_PICK
                mainActivity.selectVideoIntent.launch(intent)
            }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance(): CameraFragment {
            return CameraFragment()
        }
    }
}
