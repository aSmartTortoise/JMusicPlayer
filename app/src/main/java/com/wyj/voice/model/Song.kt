package com.wyj.voice.model

import android.os.Parcel
import android.os.Parcelable

data class Song(
    var id: Int = 0,
    val title: String?,
    var displayName: String?,
    var artist: String?,
    var album: String?,
    val path: String?,
    val duration: Int,
    val size: Int,
    var favorite: Boolean = false
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        title = parcel.readString(),
        displayName = parcel.readString(),
        artist = parcel.readString(),
        album = parcel.readString(),
        path = parcel.readString(),
        duration = parcel.readInt(),
        size = parcel.readInt(),
        favorite = parcel.readInt() != 0
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(displayName)
        parcel.writeString(artist)
        parcel.writeString(album)
        parcel.writeString(path)
        parcel.writeInt(duration)
        parcel.writeInt(size)
        parcel.writeInt(if (favorite) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<Song> {
            override fun createFromParcel(parcel: Parcel): Song = Song(parcel)
            override fun newArray(size: Int): Array<Song?> = arrayOfNulls(size)
        }
    }
}
