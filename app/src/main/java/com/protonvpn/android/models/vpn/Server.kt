/*
 * Copyright (c) 2018 Proton Technologies AG
 * 
 * This file is part of ProtonVPN.
 * 
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.protonvpn.android.models.vpn

import android.content.Context
import androidx.annotation.ColorRes
import com.protonvpn.android.R
import com.protonvpn.android.components.Listable
import com.protonvpn.android.components.Markable
import com.protonvpn.android.utils.CountryTools
import com.protonvpn.android.utils.DebugUtils.debugAssert
import com.protonvpn.android.utils.implies
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.proton.core.network.data.protonApi.IntToBoolSerializer
import java.util.regex.Pattern

@Serializable
data class Server(
    @SerialName(value = "ID") val serverId: String,
    @SerialName(value = "EntryCountry") val entryCountry: String? = null,
    @SerialName(value = "ExitCountry") val exitCountry: String,
    @SerialName(value = "Name") val serverName: String,
    @SerialName(value = "Servers") val connectingDomains: List<ConnectingDomain>,
    @SerialName(value = "Domain") val domain: String,
    @SerialName(value = "Load") var load: Float,
    @SerialName(value = "Tier") val tier: Int,
    @SerialName(value = "Region") val region: String?,
    @SerialName(value = "City") val city: String?,
    @SerialName(value = "Features") val features: Int,
    @SerialName(value = "Location") private val location: Location,

    @SerialName(value = "Score") var score: Float,

    @Serializable(with = IntToBoolSerializer::class)
    @SerialName(value = "Status")
    private val isOnline: Boolean
) : Markable, java.io.Serializable, Listable {

    val online get() = isOnline && connectingDomains.any { it.isOnline }

    @Transient
    private val translatedCoordinates: TranslatedCoordinates = TranslatedCoordinates(exitCountry)

    @Transient
    val keywords: List<String> = mutableListOf<String>().apply {
        if (features and 4 == 4)
            add("p2p")
        if (features and 2 == 2)
            add("tor")
    }

    @Transient
    val entryCountryCoordinates: TranslatedCoordinates? =
            if (entryCountry != null) TranslatedCoordinates(this.entryCountry) else null

    init {
        debugAssert {
            isOnline.implies(connectingDomains.any(ConnectingDomain::isOnline))
        }
    }

    val isFreeServer: Boolean
        get() = domain.contains("-free")

    val flag: String
        get() = if (exitCountry == "GB") "UK" else exitCountry

    val isBasicServer: Boolean
        get() = tier == 1

    val isPlusServer: Boolean
        get() = tier == 2

    val isPMTeamServer: Boolean
        get() = tier == 3

    val loadColor: Int
        @ColorRes
        get() = when {
            load < 50f -> R.color.colorAccent
            load < 90f -> R.color.yellow
            else -> R.color.dimmedRed
        }

    val isSecureCoreServer: Boolean
        get() = features and 1 == 1

    val serverNumber: Int
        get() {
            val name = serverName
            val pattern = Pattern.compile("#(\\d+(\\d+)?)")
            val m = pattern.matcher(name)
            return if (m.find()) {
                Integer.valueOf(m.group(1))
            } else {
                1
            }
        }

    private val secureCoreServerNaming: String
        get() = CountryTools.getFullName(entryCountry) + " >> " + CountryTools.getFullName(
                exitCountry)

    val displayName: String get() = if (isSecureCoreServer)
        secureCoreServerNaming
    else
        CountryTools.getFullName(flag)

    override fun getCoordinates(): TranslatedCoordinates = translatedCoordinates

    override fun isSecureCoreMarker() = false

    override fun getMarkerText(): String = secureCoreServerNaming

    override fun getConnectableServers(): List<Server> = listOf(this)

    val onlineConnectingDomains get() = connectingDomains.filter(ConnectingDomain::isOnline)

    fun getRandomConnectingDomain() = onlineConnectingDomains.random()

    override fun toString() = "$domain $entryCountry"

    override fun getLabel(context: Context): String = if (isSecureCoreServer)
        CountryTools.getFullName(entryCountry) else serverName
}
