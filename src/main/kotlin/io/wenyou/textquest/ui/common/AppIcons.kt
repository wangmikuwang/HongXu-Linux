package io.wenyou.textquest.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Bundled Ionicons outlines. Original geometry and MIT notice: third_party/ionicons. */
object AppIcons {
    val Add: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Add", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M256,112 L256,400").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M400,256 L112,256").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder("Ionicons.ArrowBack", 24.dp, 24.dp, 512f, 512f, autoMirror = true).apply {
            addPath(PathParser().parsePathString("M328,112 L184,256 L328,400").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 48f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val ArrowForward: ImageVector by lazy {
        ImageVector.Builder("Ionicons.ArrowForward", 24.dp, 24.dp, 512f, 512f, autoMirror = true).apply {
            addPath(PathParser().parsePathString("M184,112 L328,256 L184,400").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 48f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Build: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Build", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M436.67,184.11a27.17,27.17,0,0,1-38.3,0l-22.48-22.49a27.15,27.15,0,0,1,0-38.29l50.89-50.89a.85.85,0,0,0-.26-1.38C393.68,57,351.09,64.15,324.05,91c-25.88,25.69-27.35,64.27-17.87,98a27,27,0,0,1-7.67,27.14l-173,160.76a40.76,40.76,0,1,0,57.57,57.54l162.15-173.3A27,27,0,0,1,372,253.44c33.46,8.94,71.49,7.26,97.07-17.94,27.49-27.08,33.42-74.94,20.1-102.33a.85.85,0,0,0-1.36-.22Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M224,284c-17.48-17-25.49-24.91-31-30.29a18.24,18.24,0,0,1-3.33-21.35,20.76,20.76,0,0,1,3.5-4.62l15.68-15.29a18.66,18.66,0,0,1,5.63-3.87,18.11,18.11,0,0,1,20,3.62c5.45,5.29,15.43,15,33.41,32.52").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M317.07,291.3c40.95,38.1,90.62,83.27,110,99.41a13.46,13.46,0,0,1,.94,19.92L394.63,444a14,14,0,0,1-20.29-.76c-16.53-19.18-61.09-67.11-99.27-107").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M17.34,193.5l29.41-28.74a4.71,4.71,0,0,1,3.41-1.35,4.85,4.85,0,0,1,3.41,1.35h0a9.86,9.86,0,0,0,8.19,2.77c3.83-.42,7.92-1.6,10.57-4.12,6-5.8-.94-17.23,4.34-24.54a207,207,0,0,1,19.78-22.6c6-5.88,29.84-28.32,69.9-44.45A107.31,107.31,0,0,1,206.67,64c22.59,0,40,10,46.26,15.67a89.54,89.54,0,0,1,10.28,11.64A78.92,78.92,0,0,0,254,88.54,68.82,68.82,0,0,0,234,87.28c-13.33,1.09-29.41,7.26-38,14-13.9,11-19.87,25.72-20.81,44.71-.68,14.12,2.72,22.1,36.1,55.49a6.6,6.6,0,0,1-.34,9.16l-18.22,18a6.88,6.88,0,0,1-9.54.09c-21.94-21.94-36.65-33.09-45-38.16s-15.07-6.5-18.3-6.85a30.85,30.85,0,0,0-18.27,3.87,11.39,11.39,0,0,0-2.64,2,14.14,14.14,0,0,0,.42,20.08l1.71,1.6a4.63,4.63,0,0,1,0,6.64L71.73,246.6A4.71,4.71,0,0,1,68.32,248a4.86,4.86,0,0,1-3.41-1.35L17.34,200.22A4.88,4.88,0,0,1,17.34,193.5Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Check: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Check", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M416,128 L192,384 L96,288").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Close: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Close", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M368,368 L144,144").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M368,144 L144,368").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Create: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Create", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M384,224V408a40,40,0,0,1-40,40H104a40,40,0,0,1-40-40V168a40,40,0,0,1,40-40H271.48").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M459.94,53.25a16.06,16.06,0,0,0-23.22-.56L424.35,65a8,8,0,0,0,0,11.31l11.34,11.32a8,8,0,0,0,11.34,0l12.06-12C465.19,69.54,465.76,59.62,459.94,53.25Z").toNodes(),
                fill = SolidColor(Color.Black), stroke = null, strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M399.34,90,218.82,270.2a9,9,0,0,0-2.31,3.93L208.16,299a3.91,3.91,0,0,0,4.86,4.86l24.85-8.35a9,9,0,0,0,3.93-2.31L422,112.66A9,9,0,0,0,422,100L412.05,90A9,9,0,0,0,399.34,90Z").toNodes(),
                fill = SolidColor(Color.Black), stroke = null, strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Delete: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Delete", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M112,112l20,320c.95,18.49,14.4,32,32,32H348c17.67,0,30.87-13.51,32-32l20-320").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M80,112 L432,112").toNodes(),
                fill = SolidColor(Color.Black), stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M192,112V72h0a23.93,23.93,0,0,1,24-24h80a23.93,23.93,0,0,1,24,24h0v40").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M256,176 L256,400").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M184,176 L192,400").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M328,176 L320,400").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Edit: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Edit", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M384,224V408a40,40,0,0,1-40,40H104a40,40,0,0,1-40-40V168a40,40,0,0,1,40-40H271.48").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M459.94,53.25a16.06,16.06,0,0,0-23.22-.56L424.35,65a8,8,0,0,0,0,11.31l11.34,11.32a8,8,0,0,0,11.34,0l12.06-12C465.19,69.54,465.76,59.62,459.94,53.25Z").toNodes(),
                fill = SolidColor(Color.Black), stroke = null, strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M399.34,90,218.82,270.2a9,9,0,0,0-2.31,3.93L208.16,299a3.91,3.91,0,0,0,4.86,4.86l24.85-8.35a9,9,0,0,0,3.93-2.31L422,112.66A9,9,0,0,0,422,100L412.05,90A9,9,0,0,0,399.34,90Z").toNodes(),
                fill = SolidColor(Color.Black), stroke = null, strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Home: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Home", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M80,212V448a16,16,0,0,0,16,16h96V328a24,24,0,0,1,24-24h80a24,24,0,0,1,24,24V464h96a16,16,0,0,0,16-16V212").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M480,256,266.89,52c-5-5.28-16.69-5.34-21.78,0L32,256").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M400,179 L400,64 L352,64 L352,133").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Info: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Info", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M248,64C146.39,64,64,146.39,64,248s82.39,184,184,184,184-82.39,184-184S349.61,64,248,64Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M220,220 L252,220 L252,336").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M208,340 L296,340").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M248,130a26,26,0,1,0,26,26A26,26,0,0,0,248,130Z").toNodes(),
                fill = SolidColor(Color.Black), stroke = null, strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val KeyboardArrowLeft: ImageVector by lazy {
        ImageVector.Builder("Ionicons.KeyboardArrowLeft", 24.dp, 24.dp, 512f, 512f, autoMirror = true).apply {
            addPath(PathParser().parsePathString("M328,112 L184,256 L328,400").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 48f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val KeyboardArrowRight: ImageVector by lazy {
        ImageVector.Builder("Ionicons.KeyboardArrowRight", 24.dp, 24.dp, 512f, 512f, autoMirror = true).apply {
            addPath(PathParser().parsePathString("M184,112 L328,256 L184,400").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 48f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val List: ImageVector by lazy {
        ImageVector.Builder("Ionicons.List", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M144.0,48.0 H368.0 a48.0,48.0 0 0,1 48.0,48.0 V416.0 a48.0,48.0 0 0,1 -48.0,48.0 H144.0 a48.0,48.0 0 0,1 -48.0,-48.0 V96.0 a48.0,48.0 0 0,1 48.0,-48.0 Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M176,128 L336,128").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M176,208 L336,208").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M176,288 L256,288").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val MoreVert: ImageVector by lazy {
        ImageVector.Builder("Ionicons.MoreVert", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M224.0,256.0 a32.0,32.0 0 1,0 64.0,0 a32.0,32.0 0 1,0 -64.0,0 Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M384.0,256.0 a32.0,32.0 0 1,0 64.0,0 a32.0,32.0 0 1,0 -64.0,0 Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M64.0,256.0 a32.0,32.0 0 1,0 64.0,0 a32.0,32.0 0 1,0 -64.0,0 Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Person: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Person", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M344,144c-3.92,52.87-44,96-88,96s-84.15-43.12-88-96c-4-55,35-96,88-96S348,90,344,144Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M256,304c-87,0-175.3,48-191.64,138.6C62.39,453.52,68.57,464,80,464H432c11.44,0,17.62-10.48,15.65-21.4C431.3,352,343,304,256,304Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val PlayArrow: ImageVector by lazy {
        ImageVector.Builder("Ionicons.PlayArrow", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M112,111V401c0,17.44,17,28.52,31,20.16l247.9-148.37c12.12-7.25,12.12-26.33,0-33.58L143,90.84C129,82.48,112,93.56,112,111Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Refresh: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Refresh", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M320,146s24.36-12-64-12A160,160,0,1,0,416,294").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M256,58 L336,138 L256,218").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Search: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Search", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M221.09,64A157.09,157.09,0,1,0,378.18,221.09,157.1,157.1,0,0,0,221.09,64Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M338.29,338.29 L448,448").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Send: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Send", 24.dp, 24.dp, 512f, 512f, autoMirror = true).apply {
            addPath(PathParser().parsePathString("M53.12,199.94l400-151.39a8,8,0,0,1,10.33,10.33l-151.39,400a8,8,0,0,1-15-.34L229.66,292.45a16,16,0,0,0-10.11-10.11L53.46,215A8,8,0,0,1,53.12,199.94Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M460,52 L227,285").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Settings: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Settings", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M262.29,192.31a64,64,0,1,0,57.4,57.4A64.13,64.13,0,0,0,262.29,192.31ZM416.39,256a154.34,154.34,0,0,1-1.53,20.79l45.21,35.46A10.81,10.81,0,0,1,462.52,326l-42.77,74a10.81,10.81,0,0,1-13.14,4.59l-44.9-18.08a16.11,16.11,0,0,0-15.17,1.75A164.48,164.48,0,0,1,325,400.8a15.94,15.94,0,0,0-8.82,12.14l-6.73,47.89A11.08,11.08,0,0,1,298.77,470H213.23a11.11,11.11,0,0,1-10.69-8.87l-6.72-47.82a16.07,16.07,0,0,0-9-12.22,155.3,155.3,0,0,1-21.46-12.57,16,16,0,0,0-15.11-1.71l-44.89,18.07a10.81,10.81,0,0,1-13.14-4.58l-42.77-74a10.8,10.8,0,0,1,2.45-13.75l38.21-30a16.05,16.05,0,0,0,6-14.08c-.36-4.17-.58-8.33-.58-12.5s.21-8.27.58-12.35a16,16,0,0,0-6.07-13.94l-38.19-30A10.81,10.81,0,0,1,49.48,186l42.77-74a10.81,10.81,0,0,1,13.14-4.59l44.9,18.08a16.11,16.11,0,0,0,15.17-1.75A164.48,164.48,0,0,1,187,111.2a15.94,15.94,0,0,0,8.82-12.14l6.73-47.89A11.08,11.08,0,0,1,213.23,42h85.54a11.11,11.11,0,0,1,10.69,8.87l6.72,47.82a16.07,16.07,0,0,0,9,12.22,155.3,155.3,0,0,1,21.46,12.57,16,16,0,0,0,15.11,1.71l44.89-18.07a10.81,10.81,0,0,1,13.14,4.58l42.77,74a10.8,10.8,0,0,1-2.45,13.75l-38.21,30a16.05,16.05,0,0,0-6.05,14.08C416.17,247.67,416.39,251.83,416.39,256Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Share: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Share", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M336,192h40a40,40,0,0,1,40,40V424a40,40,0,0,1-40,40H136a40,40,0,0,1-40-40V232a40,40,0,0,1,40-40h40").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M336,128 L256,48 L176,128").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M256,321 L256,48").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
    val Star: ImageVector by lazy {
        ImageVector.Builder("Ionicons.Star", 24.dp, 24.dp, 512f, 512f, autoMirror = false).apply {
            addPath(PathParser().parsePathString("M259.92,262.91,216.4,149.77a9,9,0,0,0-16.8,0L156.08,262.91a9,9,0,0,1-5.17,5.17L37.77,311.6a9,9,0,0,0,0,16.8l113.14,43.52a9,9,0,0,1,5.17,5.17L199.6,490.23a9,9,0,0,0,16.8,0l43.52-113.14a9,9,0,0,1,5.17-5.17L378.23,328.4a9,9,0,0,0,0-16.8L265.09,268.08A9,9,0,0,1,259.92,262.91Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M108,68 L88,16 L68,68 L16,88 L68,108 L88,160 L108,108 L160,88 L108,68Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
            addPath(PathParser().parsePathString("M426.67,117.33 L400,48 L373.33,117.33 L304,144 L373.33,170.67 L400,240 L426.67,170.67 L496,144 L426.67,117.33Z").toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 32f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero)
        }.build()
    }
}
