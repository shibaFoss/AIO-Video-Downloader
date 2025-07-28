package lib.files

object FileExtensions {
    @JvmStatic
    val ARCHIVE_EXTENSIONS = arrayOf(
        "7z", "ace", "apk", "arj", "bin", "bz2", "bzip", "bzip2", "cab", "cb7", "cbr", "cbz",
        "cpgz", "cpio", "cpt", "dmg", "gz", "hfs", "iso", "jar", "jks", "jtar", "ks", "lz", "lz4",
        "lzma", "lzip", "lzf", "lzop", "nsf", "p7m", "p7s", "pkg", "rar", "r00", "r01", "r02", "r03",
        "r04", "r05", "r06", "r07", "r08", "r09", "sitx", "squashfs", "s7z", "tar", "tar.bz", "tar.bz2",
        "tar.gz", "tar.lz", "tar.lzma", "tar.xz", "tar.z", "tbz", "tbz2", "taz", "tgz", "uue", "vhd",
        "vmdk", "war", "xapk", "xz", "z", "z7", "zpaq"
    )
    
    @JvmStatic
    val PROGRAM_EXTENSIONS = arrayOf(
        "app", "appimage", "apk", "bat", "bin", "cgi", "com", "cpl", "deb", "dmg", "exe", "jar",
        "js", "key", "msi", "msu", "pkg", "pl", "ps1", "py", "rb", "run", "rpm", "sfx", "sh", "vbe",
        "vbs", "wsf", "wsc", "xapk", "xpi"
    )
    
    @JvmStatic
    val VIDEO_EXTENSIONS = arrayOf(
        "3gp", "amv", "asf", "avi", "bik", "dat", "divx", "drc", "dvr-ms", "f4v", "flv", "h264", "hevc",
        "ivf", "m2ts", "m2v", "m3u8", "m4u", "m4v", "mkv", "mov", "mp4", "mpeg", "mpeg-4", "mpg", "mpv",
        "mts", "qt", "rm", "rmvb", "svi", "tod", "tp", "trp", "ts", "vob", "vp8", "vp9", "vpl", "vpx",
        "webm", "wmv", "xvid", "y4m", "yuv"
    )
    
    @JvmStatic
    val ONLINE_VIDEO_FORMATS = listOf(
        "3gp", "avi", "divx", "f4v", "flv", "m2ts", "m3u8", "m4v", "mkv", "mov", "mp4", "mpd", "ogv",
        "rm", "vob", "webm", "wmv"
    )
    
    @JvmStatic
    val MUSIC_EXTENSIONS = arrayOf(
        "3g2", "3gp", "aac", "ac3", "aif", "aiff", "alack", "amr", "ape", "awb", "bwf", "cda", "dct",
        "dsf", "dts", "dvf", "f4a", "flac", "gsm", "ivs", "klaxon", "m4a", "m4b", "m4p", "mid", "midi",
        "mmf", "mod", "mp3", "mpa", "mpc", "nsf", "ofr", "ogg", "omg", "opus", "pcm", "ra", "ram", "sid",
        "s3m", "sln", "spx", "tak", "vox", "wav", "wma", "wpd", "wv", "xm"
    )
    
    @JvmStatic
    val DOCUMENT_EXTENSIONS = arrayOf(
        "air", "asp", "aspx", "azw", "bak", "bin", "cfg", "cgi", "chm", "class", "cmd", "conf", "cpp",
        "csv", "doc", "docm", "docx", "dotm", "dotx", "dmp", "eml", "epub", "fdb", "fdf", "fb2", "gz",
        "h", "html", "ini", "iso", "java", "js", "jsp", "keynote", "log", "markdown", "md", "mobi",
        "msg", "numbers", "odp", "ods", "odt", "pages", "pdf", "pdfa", "php", "plist", "pot", "pps",
        "ppt", "pptm", "pptx", "properties", "ps", "py", "rb", "rtf", "swo", "swp", "swt", "tex", "txt",
        "vb", "vbs", "wpd", "wps", "xaml", "xap", "xls", "xlsm", "xlsx", "xlt", "xltx", "xml", "xps",
        "xsd", "xsf", "xsl", "xslt", "yaml", "yml"
    )
    
    @JvmStatic
    val IMAGE_EXTENSIONS = arrayOf(
        "ai", "arw", "avif", "bmp", "cr2", "cur", "dng", "eps", "exr", "fpx", "gif", "hdr", "heic",
        "heif", "ico", "img", "indd", "jpe", "jpeg", "jpg", "jfif", "kra", "nef", "nif", "orf", "pbm",
        "pcx", "pdn", "pfm", "pgm", "pict", "png", "ppm", "psb", "psd", "raw", "rw2", "sgi", "sr2",
        "svg", "tif", "tiff", "wbmp", "webp", "xcf", "xbm"
    )
    
    @JvmStatic
    val ALL_DOWNLOADABLE_FORMATS = arrayOf(
        // Executables & Installers
        "exe", "msi", "app", "apk", "jar", "bat", "sh", "com", "bin", "deb", "rpm", "dmg", "appx", "pkg",
        "xapk", "wsf", "paf", "sublime-project", "sublime-workspace", "appimage", "iso", "xpi", "hwp", "key",
        "sfx", "tgz", "cpl", "vbs", "run", "msu", "pl", "py", "rb", "js", "cgi", "vbe", "wsc", "ps1", "cmd",
        "air", "gadget", "ipa", "osx", "vb", "xap", "xpl", "ar", "bz", "bz2", "cpio", "dat", "dmp", "fdb",
        "p7m", "p7s", "xaml",
        
        // Documents & Text
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "rtf", "txt", "csv", "odt", "ods", "odp", "odg",
        "epub", "mobi", "azw", "fb2", "cbz", "cbr", "chm", "djvu", "ps", "pages", "numbers", "keynote", "tex",
        "docm", "dotx", "dotm", "xps", "fdf", "pdfa", "xml", "html", "md", "markdown", "eml", "msg", "pot",
        "pps", "pptm", "sxc", "sxi", "wpd", "wps", "xlr", "xlsm", "xlt", "xltx",
        
        // Config & Log Files
        "yaml", "yml", "properties", "conf", "ini", "cfg", "log", "bak", "backup", "plist", "tmp", "swp",
        "swo", "swt",
        
        // Code & Dev
        "java", "class", "xsd", "xsl", "xslt", "xsf", "svg",
        
        // Images
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "psd", "ai", "eps", "ico", "webp", "heic", "tif", "raw",
        "cr2", "nef", "orf", "rw2", "dng", "arw", "sr2", "indd", "kra", "pdn", "psb", "xcf", "xbm", "jpe",
        "jfif", "heif", "avif", "cur", "wbmp", "pjpeg", "pict", "hdr", "exr", "pfm", "fpx", "sgi", "pcx",
        "iff", "img", "nif", "tga", "dcm", "fit", "bpg", "jp2", "j2k", "jpf", "jpx", "dpx", "sct", "jpeg2000",
        "emf", "wmf", "ppm", "pgm", "pbm", "xpm", "xwd", "xfig", "fif",
        
        // Video
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "vob", "m4v", "mpeg", "mpg", "3gp", "rmvb", "swf",
        "asf", "m2ts", "ts", "divx", "ogv", "f4v", "mxf", "mts", "bik", "h264", "hevc", "vp9", "av1", "mpv",
        "vpl", "drc", "xvid", "svi", "qt", "rm", "tod", "trp", "vpx", "yuv", "m2v", "m4u", "tp", "y4m", "ivf",
        "ismv", "mpeg-4", "dvr-ms", "vp8",
        
        // Audio
        "mp3", "wav", "ogg", "wma", "aac", "flac", "alac", "aiff", "m4a", "opus", "pcm", "mid", "midi", "ra",
        "ram", "dts", "ac3", "ape", "au", "mka", "m4b", "spx", "wv", "sln", "tak", "vox", "bwf", "f4a", "amr",
        "awb", "cda", "dct", "dsf", "dvf", "gsm", "iklax", "ivs", "m4p", "mmf", "msv", "nmf", "ofr", "omg",
        "3g2", "3gp2", "mpa", "aif", "aifc", "mpc", "mod", "sid", "s3m", "it", "xm", "nsf", "sap",
        
        // Archives & Compressed
        "zip", "rar", "7z", "tar", "gz", "xz", "z", "war", "sitx", "tbz", "tbz2", "cpgz", "lzh", "lz", "lzma",
        "lz4", "arj", "ace", "uue", "cab", "squashfs", "s7z", "cb7", "r00", "r01", "r02", "r03", "r04", "r05",
        "r06", "r07", "r08", "r09", "tar.gz", "tar.bz2", "tar.xz", "tar.lz", "tar.lzma", "tar.z", "zpaq", "taz",
        "lzf", "lzip", "lzop", "bzip", "bzip2", "cpt", "hfs", "jks", "jtar", "ks", "tar.bz",
        
        // Disk Images & VM
        "vhd", "vmdk", "z7",
        
        // Web/Script
        "aspx", "php", "jsp", "asp",
        
        // Repeats (intentionally kept per instruction, ensures exact representation)
        "jar", "jar", "msi", "app", "exe", "dmg", "deb", "rpm", "pl", "cgi", "wsf", "vbs", "py", "rb", "js",
        "pl", "sublime-project", "sublime-workspace", "vb", "js", "pl", "py", "rb", "jar", "xml", "svg",
        "ico", "ico", "webp", "jpg", "jpeg", "png", "gif", "bmp", "tiff", "mkv", "mp4", "avi", "mov", "wmv",
        "flv", "webm", "mpg", "mpeg", "3gp", "rm", "rmvb", "xvid", "divx", "ogg", "opus", "wv", "flac", "alac",
        "m4a", "mka", "mp3", "wav", "aac", "ape", "ra", "sln", "tak", "vox", "gsm", "mod", "sid", "mpc",
        "mpc", "dct", "wav", "wv", "ogg", "aiff", "m4b", "m4p", "spx", "au", "cbz", "cbr"
    )
    
}