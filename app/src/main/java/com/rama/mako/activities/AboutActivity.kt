package com.rama.mako.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.rama.bohio.widgets.WdLabel
import com.rama.mako.CsActivity
import com.rama.mako.R
import com.rama.bohio.R as BohioR

class AboutActivity : CsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_about)

        val root = findViewById<View>(R.id.about_root)
        applyEdgeToEdgePadding(root)
        applyCurrentTheme(root)

        val closeButton = findViewById<View>(R.id.close_button)
        closeButton.setOnClickListener {
            finish()
        }

        val claimsLayout = findViewById<LinearLayout>(R.id.claims)
        val claimsData = resources.getStringArray(R.array.app_claims)
        claimsData.forEach { claim ->
            val tag = WdLabel(this)
            tag.setText(claim)
            tag.setIcon(BohioR.drawable.px_octagon_check)
            claimsLayout.addView(tag)
        }

        val contributorsLayout = findViewById<LinearLayout>(R.id.contributors)
        val contributorsNamesData = resources.getStringArray(R.array.app_contributor_names)
        val contributorsUrlData = resources.getStringArray(R.array.app_contributor_urls)
        val contributors = contributorsNamesData.zip(contributorsUrlData)

        contributors.forEachIndexed { index, (name, url) ->
            val nameTag = WdLabel(this)
            nameTag.setText(name)
            nameTag.setIcon(BohioR.drawable.px_user)

            val urlTag = WdLabel(this)
            urlTag.setText(url)
            urlTag.setIcon(BohioR.drawable.px_github)

            contributorsLayout.addView(nameTag)
            contributorsLayout.addView(urlTag)

            if (index != contributors.lastIndex) {
                val separatorView = View(this, null, 0, BohioR.style.Separator)
                contributorsLayout.addView(separatorView)
            }
        }

        val discordButton = findViewById<View>(R.id.discord_button)
        discordButton.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(getString(BohioR.string.discord_url))
            )
            startActivity(intent)
        }

//        val githubButton = findViewById<View>(R.id.github_button)
//        githubButton.setOnClickListener {
//            val rawUrl = getString(BohioR.string.product_mako_url)
//            val url =
//                if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
//            val intent = Intent(
//                Intent.ACTION_VIEW,
//                Uri.parse(url)
//            )
//            startActivity(intent)
//        }

        val version = packageManager.getPackageInfo(packageName, 0).versionCode
        val nameView = findViewById<TextView>(R.id.name_version)
        nameView.text = getString(BohioR.string.name_version, getString(R.string.app_name), version)
    }
}
