package com.rama.mako.managers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Xml
import com.rama.bohio.managers.ThemeManager
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

class IconManager(
    private val context: Context,
    private val appsProvider: AppsProvider
) {

    companion object {
        private const val MONOCHROME_SCALE = 1.5f
    }

    data class IconPackEntry(
        val packageName: String,
        val label: String,
        val icon: Drawable?
    )

    private val prefs = PrefsManager.getInstance(context)
    private val packageManager = context.packageManager
    private val iconCache = mutableMapOf<String, Drawable>()
    private val appFilterCache = mutableMapOf<String, Map<String, String>>()

    fun getIcon(app: AppsProvider.AppEntry): Drawable {
        val activity = app as? AppsProvider.ActivityEntry ?: return appsProvider.getIcon(app)
        val source = prefs.getIconSource()
        val selectedPack = prefs.getIconPackPackage()
        val cacheKey = buildCacheKey(activity, source, selectedPack)

        return iconCache.getOrPut(cacheKey) {
            when (source) {
                PrefsManager.IconSource.MONOCHROME -> getMonochromeIcon(app)
                PrefsManager.IconSource.ICON_PACK -> getIconFromPack(activity, selectedPack)
                else -> null
            } ?: appsProvider.getIcon(app)
        }
    }

    fun getInstalledIconPacks(): List<IconPackEntry> {
        val foundPackages = linkedSetOf<String>()
        val result = mutableListOf<IconPackEntry>()

        getIconPackActions().forEach { action ->
            queryIntentActivities(Intent(action)).forEach { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@forEach
                if (!foundPackages.add(packageName)) return@forEach
                if (!hasAppFilter(packageName)) return@forEach

                result.add(
                    IconPackEntry(
                        packageName = packageName,
                        label = getIconPackLabel(packageName) ?: packageName,
                        icon = runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
                    )
                )
            }
        }

        return result.sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    fun getIconPackLabel(packageName: String): String? {
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrNull()
    }

    private fun getMonochromeIcon(app: AppsProvider.AppEntry): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null

        val baseIcon = appsProvider.getIcon(app)
        val adaptiveIcon = baseIcon as? AdaptiveIconDrawable ?: return null
        val monochrome = adaptiveIcon.monochrome ?: return null
        val tintColor = resolveSystemMonochromeTintColor()
        val tintedDrawable =
            (monochrome.constantState?.newDrawable()?.mutate() ?: monochrome).apply {
                setTint(tintColor)
            }

        return ScaledDrawable(tintedDrawable, MONOCHROME_SCALE)
    }

    private fun resolveSystemMonochromeTintColor(): Int {
        val prefs = PrefsManager.getInstance(context)
        return ThemeManager.paletteFor(prefs.getTheme(), context).accent_1
    }

    private fun getIconFromPack(
        app: AppsProvider.ActivityEntry,
        packageName: String
    ): Drawable? {
        if (packageName.isBlank()) return null

        val drawableName = resolvePackDrawableName(packageName, app.activityInfo.componentName)

        return runCatching {
            val resources = packageManager.getResourcesForApplication(packageName)

            var drawableId = resources.getIdentifier(drawableName, "drawable", packageName)
            if (drawableId == 0) {
                drawableId = resources.getIdentifier(drawableName, "mipmap", packageName)
            }
            if (drawableId == 0) return null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                resources.getDrawable(drawableId, null)
            } else {
                @Suppress("DEPRECATION")
                resources.getDrawable(drawableId)
            }
        }.getOrNull()
    }

    private fun resolvePackDrawableName(
        packageName: String,
        componentName: ComponentName
    ): String? {
        val appFilterMap = appFilterCache.getOrPut(packageName) { loadAppFilter(packageName) }
        if (appFilterMap.isEmpty()) return null

        buildComponentLookupKeys(componentName).forEach { key ->
            appFilterMap[key]?.let { drawableName ->
                return drawableName
            }
        }

        return null
    }

    private fun buildComponentLookupKeys(componentName: ComponentName): List<String> {
        val packageName = componentName.packageName
        val fullClassName = componentName.className
        val shortClassName = if (fullClassName.startsWith("$packageName.")) {
            fullClassName.removePrefix(packageName)
        } else {
            fullClassName
        }

        val candidates = listOf(
            "ComponentInfo{$packageName/$fullClassName}",
            "ComponentInfo{$packageName/$shortClassName}",
            "$packageName/$fullClassName",
            "$packageName/$shortClassName"
        )

        return candidates.mapNotNull { normalizeComponent(it) }.distinct()
    }

    private fun hasAppFilter(packageName: String): Boolean {
        val appFilterMap = appFilterCache.getOrPut(packageName) { loadAppFilter(packageName) }
        return appFilterMap.isNotEmpty()
    }

    private fun loadAppFilter(packageName: String): Map<String, String> {
        val fromXmlResources = loadAppFilterFromXmlResources(packageName)
        if (fromXmlResources.isNotEmpty()) {
            return fromXmlResources
        }

        return loadAppFilterFromAssets(packageName)
    }

    private fun loadAppFilterFromXmlResources(packageName: String): Map<String, String> {
        return runCatching {
            val resources = packageManager.getResourcesForApplication(packageName)
            val appFilterId = resources.getIdentifier("appfilter", "xml", packageName)
            if (appFilterId == 0) return emptyMap()

            val parser = resources.getXml(appFilterId)
            try {
                parseAppFilter(parser)
            } finally {
                parser.close()
            }
        }.getOrElse { emptyMap() }
    }

    private fun loadAppFilterFromAssets(packageName: String): Map<String, String> {
        return runCatching {
            val packageContext =
                context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
            packageContext.assets.open("appfilter.xml").use { stream ->
                val parser = Xml.newPullParser()
                parser.setInput(stream, "utf-8")
                parseAppFilter(parser)
            }
        }.getOrElse { emptyMap() }
    }

    private fun parseAppFilter(parser: XmlPullParser): Map<String, String> {
        val appFilterMap = mutableMapOf<String, String>()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals(
                    "item",
                    ignoreCase = true
                )
            ) {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")

                val normalizedComponent = normalizeComponent(component)
                if (normalizedComponent != null && !drawable.isNullOrBlank()) {
                    appFilterMap[normalizedComponent] = drawable.trim()
                }
            }

            eventType = parser.next()
        }

        return appFilterMap
    }

    private fun normalizeComponent(component: String?): String? {
        if (component.isNullOrBlank()) return null

        var normalized = component.trim()
        if (normalized.startsWith("ComponentInfo{") && normalized.endsWith("}")) {
            normalized = normalized.removePrefix("ComponentInfo{").removeSuffix("}")
        }

        val slashIndex = normalized.indexOf('/')
        if (slashIndex <= 0 || slashIndex == normalized.lastIndex) return null

        val packageName = normalized.substring(0, slashIndex).trim()
        var className = normalized.substring(slashIndex + 1).trim()
        if (packageName.isEmpty() || className.isEmpty()) return null

        className = when {
            className.startsWith(".") -> packageName + className
            className.startsWith(packageName) -> className
            else -> "$packageName.$className"
        }

        return "${packageName.lowercase(Locale.ROOT)}/${className.lowercase(Locale.ROOT)}"
    }

    private fun queryIntentActivities(intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
    }

    private fun buildCacheKey(
        app: AppsProvider.ActivityEntry,
        source: String,
        selectedPack: String
    ): String {
        return "$source:$selectedPack:${app.packageName}:${app.activityInfo.componentName.className}:${app.userHandle.hashCode()}"
    }

    private fun getIconPackActions(): List<String> {
        return listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.gau.go.launcherex.theme"
        )
    }

    private class ScaledDrawable(
        private val drawable: Drawable,
        private val scale: Float
    ) : Drawable() {

        override fun draw(canvas: Canvas) {
            val bounds = bounds
            val saveCount = canvas.save()
            canvas.scale(scale, scale, bounds.exactCenterX(), bounds.exactCenterY())
            drawable.bounds = bounds
            drawable.draw(canvas)
            canvas.restoreToCount(saveCount)
        }

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            drawable.bounds = bounds
        }

        override fun setAlpha(alpha: Int) {
            drawable.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            drawable.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        override fun getIntrinsicWidth(): Int = drawable.intrinsicWidth

        override fun getIntrinsicHeight(): Int = drawable.intrinsicHeight
    }
}
