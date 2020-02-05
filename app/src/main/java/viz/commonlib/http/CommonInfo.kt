package viz.commonlib.http

data class CommonInfo(
    var value: String,
    var excludeUrls: MutableList<String> = mutableListOf(),
    var includeUrls: MutableList<String> = mutableListOf()
)