package com.example.project.Patient

import android.os.Parcel
import android.os.Parcelable

/**
 * Data class representing a doctor's profile, implementing [Parcelable] for efficient
 * data transfer between Android components.
 *
 * This class holds comprehensive information about a doctor, including personal details,
 * professional background, contact information, and ratings.
 *
 * @property Address The physical address of the doctor's practice. Defaults to an empty string.
 * @property Biography A brief biography or description of the doctor. Defaults to an empty string.
 * @property Id The unique integer identifier for the doctor. Defaults to 0.
 * @property Name The full name of the doctor. Defaults to an empty string.
 * @property Picture The URL or path to the doctor's profile picture. Defaults to an empty string.
 * @property Special The doctor's specialization (e.g., "Cardiologist"). Defaults to an empty string.
 * @property Expriense The number of years of experience the doctor has. Defaults to 0.
 * @property Location A geographical location string or URL (e.g., Google Maps link). Defaults to an empty string.
 * @property Mobile The doctor's mobile phone number. Defaults to an empty string.
 * @property Patiens A string representing the number of patients (e.g., "1000+"). Defaults to an empty string.
 * @property Rating The doctor's average rating. Defaults to 0.0.
 * @property Site The URL of the doctor's website. Defaults to an empty string.
 */
data class DoctorsModel(
    val Address:String="",
    val Biography:String="",
    val Id:Int=0,
    val Name:String="",
    val Picture:String="",
    val Special:String="",
    val Expriense:Int=0,
    val Location:String="",
    val Mobile:String="",
    val Patiens:String="",
    val Rating:Double=0.0,
    val Site:String=""
):Parcelable {
    /**
     * Secondary constructor used for recreating the object from a [Parcel].
     *
     * @param parcel The Parcel from which to read the object's data.
     */
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readInt(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readInt(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readDouble(),
        parcel.readString().toString()
    ) {
    }

    /**
     * Writes the object's data to a [Parcel].
     *
     * @param parcel The Parcel to which the object's data will be written.
     * @param flags Additional flags about how the object should be written.
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(Address)
        parcel.writeString(Biography)
        parcel.writeInt(Id)
        parcel.writeString(Name)
        parcel.writeString(Picture)
        parcel.writeString(Special)
        parcel.writeInt(Expriense)
        parcel.writeString(Location)
        parcel.writeString(Mobile)
        parcel.writeString(Patiens)
        parcel.writeDouble(Rating)
        parcel.writeString(Site)
    }

    /**
     * Describes the kinds of special objects contained in this Parcelable instance's marshaled representation.
     *
     * @return A bitmask indicating the set of special object types marshaled by this Parcelable object.
     */
    override fun describeContents(): Int {
        return 0
    }

    /**
     * Companion object to generate instances of [DoctorsModel] from a [Parcel].
     */
    companion object CREATOR : Parcelable.Creator<DoctorsModel> {
        /**
         * Creates a new instance of the Parcelable class, instantiating it from the given Parcel whose data had
         * previously been written by [writeToParcel].
         *
         * @param parcel The Parcel to unmarshall a new object from.
         * @return A new instance of the Parcelable class.
         */
        override fun createFromParcel(parcel: Parcel): DoctorsModel {
            return DoctorsModel(parcel)
        }

        /**
         * Creates a new array of the Parcelable class.
         *
         * @param size The size of the array to create.
         * @return An array of the Parcelable class, with every entry initialized to null.
         */
        override fun newArray(size: Int): Array<DoctorsModel?> {
            return arrayOfNulls(size)
        }
    }
}