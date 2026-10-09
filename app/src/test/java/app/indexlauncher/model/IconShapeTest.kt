package app.indexlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class IconShapeTest {
    @Test fun storedShapesRoundTripAndInvalidSettingsUseDeviceDefault() {
        IconShape.entries.forEach { shape ->
            assertEquals(shape, IconShape.fromStored(shape.name))
        }
        assertEquals(IconShape.DeviceDefault, IconShape.fromStored(null))
        assertEquals(IconShape.DeviceDefault, IconShape.fromStored("unrecognized"))
    }
}
