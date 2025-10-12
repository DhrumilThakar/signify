package com.example.signify

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.signify.databinding.FragmentCameraBinding

// 1. Implement the ServerResultCallback interface
class CameraFragment : Fragment(), ServerResultCallback {

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

        // 2. Register this fragment as the callback receiver
        ServerClient.getInstance().registerCallback(this)

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

    // 3. Unregister the callback when the view is destroyed to prevent memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        ServerClient.getInstance().unregisterCallback()
    }

    // 4. Implement the displayResponse method
    override fun displayResponse(result: String, isGloss: Boolean) {
        // This is now called on the main thread.
        // Check if the fragment's view is still available
        if (view != null) {
            // Example: Update a TextView in your fragment's layout
            // binding.predictionTextView.text = result
        }
    }

    // 5. Implement the addNewTranscript method
    override fun addNewTranscript(transcript: String) {
        // This is now called on the main thread.
        // Check if the fragment's view is still available
        if (view != null) {
            // Example: Update another TextView
            // binding.transcriptTextView.append(transcript + "\n")
        }
    }

    // 6. Implement the onConnected method
    override fun onConnected(isSuccess: Boolean) {
        // This is now called on the main thread.
        // Handle UI changes based on connection status if needed
    }


    companion object {
        @JvmStatic
        fun newInstance(): CameraFragment {
            return CameraFragment()
        }
    }
}
