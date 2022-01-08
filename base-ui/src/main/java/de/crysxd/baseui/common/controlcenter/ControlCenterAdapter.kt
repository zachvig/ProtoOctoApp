package de.crysxd.baseui.common.controlcenter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.baseui.common.ViewBindingHolder
import de.crysxd.baseui.databinding.ControleCenterItemBinding
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.octoprint.models.socket.Message

class ControlCenterAdapter : RecyclerView.Adapter<ControlCenterAdapter.ViewHolder>() {

    var instances = listOf<Pair<OctoPrintInstanceInformationV3, Message.CurrentMessage?>>()

    override fun getItemCount() = instances.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.label.text = instances[position].first.label
        holder.binding.details.text = "Connecting..."
        holder.binding.webcam.smallMode = true
        holder.binding.webcam.canSwitchWebcam = false
        holder.binding.content.clipToOutline = true
    }

    class ViewHolder(parent: ViewGroup) : ViewBindingHolder<ControleCenterItemBinding>(
        ControleCenterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
}