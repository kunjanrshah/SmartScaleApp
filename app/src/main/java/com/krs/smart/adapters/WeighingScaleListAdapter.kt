package com.krs.smart.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.krs.smart.MainActivity
import com.krs.smart.R
import com.krs.smart.adapters.viewholders.WeighingScaleViewHolder
import com.krs.smart.callbacks.ControlButtonsClickListener
import com.krs.smart.room.model.ScaleType
import com.krs.smart.room.model.WeighingScale
import java.util.*


class WeighingScaleListAdapter(listener: ControlButtonsClickListener) :
    RecyclerView.Adapter<WeighingScaleViewHolder>() {
    private var weighingScales: MutableList<WeighingScale> = arrayListOf()
    private var controlButtonsClickListener = listener
    var bleScaleWeight:TextView?=null
    var wifiScaleWeight:TextView?=null
    var mqttScaleWeight:TextView?=null
    var wifiScaleName:TextView?=null

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
            bleScaleWeight=holder.weighingScaleBinding.scaleWeight
            if(MainActivity.bluetoothDevice!=null){
                holder.weighingScaleBinding.scale?.name= MainActivity.bluetoothDevice?.name.toString()+" "+ MainActivity.bluetoothDevice?.address.toString()
            }
            blePos=position
        } else if(holder.weighingScaleBinding.scale?.type==ScaleType.WIFI){
            wifiScaleWeight = holder.weighingScaleBinding.scaleWeight
            wifiScaleName=holder.weighingScaleBinding.scaleTitle
        }else if(holder.weighingScaleBinding.scale?.type==ScaleType.INTERNET){
            mqttScaleWeight=holder.weighingScaleBinding.scaleWeight
            holder.weighingScaleBinding.scale?.name=""
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