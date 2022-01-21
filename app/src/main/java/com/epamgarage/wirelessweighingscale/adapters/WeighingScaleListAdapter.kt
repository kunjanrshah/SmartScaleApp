package com.epamgarage.wirelessweighingscale.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.epamgarage.wirelessweighingscale.MainActivity
import com.epamgarage.wirelessweighingscale.R
import com.epamgarage.wirelessweighingscale.adapters.viewholders.WeighingScaleViewHolder
import com.epamgarage.wirelessweighingscale.callbacks.ControlButtonsClickListener
import com.epamgarage.wirelessweighingscale.room.model.ScaleType
import com.epamgarage.wirelessweighingscale.room.model.WeighingScale
import java.util.*


class WeighingScaleListAdapter(listener: ControlButtonsClickListener) :
    RecyclerView.Adapter<WeighingScaleViewHolder>() {
    private var weighingScales: MutableList<WeighingScale> = arrayListOf()
    private var controlButtonsClickListener = listener
    var bleTextView:TextView?=null
    lateinit var wifiTextView:TextView
    lateinit var wifiTextViewName:TextView

    private var context: Context? =null
    var blePos:Int=-1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeighingScaleViewHolder {
        return WeighingScaleViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.layout_weighing_scale, parent, false
            )
        )
    }

    fun setContext(activity: MainActivity){
        context=activity
    }

    override fun onBindViewHolder(holder: WeighingScaleViewHolder, position: Int) {

        holder.weighingScaleBinding.let {
            it.position = position
            it.scale = weighingScales[position]
            it.callback = controlButtonsClickListener
        }
        if (holder.weighingScaleBinding.scale?.type==ScaleType.BLUETOOTH){
            bleTextView=holder.weighingScaleBinding.scaleWeight
            if(MainActivity.bluetoothDevice!=null){
                holder.weighingScaleBinding.scale?.name= MainActivity.bluetoothDevice?.address.toString()
            }
            blePos=position
        } else if(holder.weighingScaleBinding.scale?.type==ScaleType.WIFI){
            wifiTextView = holder.weighingScaleBinding.scaleWeight
            wifiTextViewName=holder.weighingScaleBinding.scaleTitle
        }
    }

    override fun getItemCount(): Int {
        return weighingScales.size
    }

    private fun getScaleTitle(): String {
        return "Indicator ${itemCount + 1}"
    }

    infix fun getItemAtPosition(position: Int) = weighingScales[position]

    @SuppressLint("NotifyDataSetChanged")
    fun setAllScales(scales: MutableList<WeighingScale>) {
        this.weighingScales = scales
        notifyDataSetChanged()
    }

    fun getWeighingScale(scaleType: ScaleType) = WeighingScale(
        View.GONE,
        "0.00",
        getScaleTitle(),
        scaleType,
        Calendar.getInstance().timeInMillis
    )
}