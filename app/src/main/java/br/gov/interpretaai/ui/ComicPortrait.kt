package br.gov.interpretaai.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.gov.interpretaai.R
import br.gov.interpretaai.ui.theme.ComicInk

@Composable
fun ComicPortrait(sceneIndex: Int, description: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(3.dp, ComicInk)
    ) {
        Image(
            painter = painterResource(sceneDrawable(sceneIndex)),
            contentDescription = description,
            modifier = Modifier.fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(17.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@DrawableRes
private fun sceneDrawable(index: Int): Int = when (index) {
    0 -> R.drawable.comic_scene_1_v2
    1 -> R.drawable.comic_scene_2
    2 -> R.drawable.comic_scene_3
    3 -> R.drawable.comic_scene_4
    else -> R.drawable.comic_scene_5
}
