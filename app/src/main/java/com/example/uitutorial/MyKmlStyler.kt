package com.example.uitutorial

import android.graphics.Color
import org.osmdroid.bonuspack.kml.KmlFeature
import org.osmdroid.bonuspack.kml.KmlLineString
import org.osmdroid.bonuspack.kml.KmlPlacemark
import org.osmdroid.bonuspack.kml.KmlPoint
import org.osmdroid.bonuspack.kml.KmlPolygon
import org.osmdroid.bonuspack.kml.KmlTrack
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline


class MyKmlStyler : KmlFeature.Styler {
    override fun onFeature(p0: Overlay?, p1: KmlFeature?) {
        TODO("Not yet implemented")
    }

    override fun onPoint(p0: Marker?, p1: KmlPlacemark?, p2: KmlPoint?) {
        TODO("Not yet implemented")
    }

    override fun onLineString(p0: Polyline?, p1: KmlPlacemark?, p2: KmlLineString?) {
        p0!!.width = Math.max(p2!!.mCoordinates.size / 200.0f, 3.0f)
        p0!!.color = Color.GREEN
    }

    override fun onPolygon(p0: Polygon?, p1: KmlPlacemark?, p2: KmlPolygon?) {
        TODO("Not yet implemented")
    }

    override fun onTrack(p0: Polyline?, p1: KmlPlacemark?, p2: KmlTrack?) {
        TODO("Not yet implemented")
    }
}