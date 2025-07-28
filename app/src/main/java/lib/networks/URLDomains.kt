package lib.networks

/**
 * Utility object containing common top-level domains (TLDs) used for URL processing.
 * This collection includes generic TLDs (gTLDs), country-code TTLDs (ccTLDs),
 * and other specialized domains.
 */
object URLDomains {
    
    /**
     * An array of common top-level domains (TLDs) used across the internet.
     * Includes:
     * - Generic TLDs (.com, .net, .org, etc.)
     * - Country-code TLDs (.us, .uk, .ca, etc.)
     * - Specialized TLDs (.edu, .gov, .mil, etc.)
     * - Newer generic TLDs (.app, .blog, .shop, etc.)
     *
     * This list is sorted alphabetically for easier maintenance and lookup.
     */
    @JvmStatic
    val TopLevelDomains: Array<String> = arrayOf(
        // Generic TLDs
        ".com", ".net", ".org", ".io", ".co", ".biz", ".info", ".mobi", ".name", ".pro",
        ".tel", ".travel", ".xxx", ".aero", ".asia", ".cat", ".coop", ".jobs", ".museum",
        ".post",
        
        // Country-code TLDs (sorted alphabetically by country code)
        ".ac", ".ad", ".ae", ".af", ".ag", ".ai", ".al", ".am", ".ao", ".aq", ".ar", ".as",
        ".at", ".au", ".aw", ".ax", ".az", ".ba", ".bb", ".bd", ".be", ".bf", ".bg", ".bh",
        ".bi", ".bj", ".bl", ".bm", ".bn", ".bo", ".bq", ".br", ".bs", ".bt", ".bv", ".bw",
        ".by", ".bz", ".ca", ".cc", ".cd", ".cf", ".cg", ".ch", ".ci", ".ck", ".cl", ".cm",
        ".cn", ".co", ".cr", ".cu", ".cv", ".cw", ".cx", ".cy", ".cz", ".de", ".dj", ".dk",
        ".dm", ".do", ".dz", ".ec", ".ee", ".eg", ".eh", ".er", ".es", ".et", ".eu", ".fi",
        ".fj", ".fk", ".fm", ".fo", ".fr", ".ga", ".gb", ".gd", ".ge", ".gf", ".gg", ".gh",
        ".gi", ".gl", ".gm", ".gn", ".gp", ".gq", ".gr", ".gt", ".gu", ".gw", ".gy", ".hk",
        ".hm", ".hn", ".hr", ".ht", ".hu", ".id", ".ie", ".il", ".im", ".in", ".io", ".iq",
        ".ir", ".is", ".it", ".je", ".jm", ".jo", ".jp", ".ke", ".kg", ".kh", ".ki", ".km",
        ".kn", ".kp", ".kr", ".kw", ".ky", ".kz", ".la", ".lb", ".lc", ".li", ".lk", ".lr",
        ".ls", ".lt", ".lu", ".lv", ".ly", ".ma", ".mc", ".md", ".me", ".mf", ".mg", ".mh",
        ".mk", ".ml", ".mm", ".mn", ".mo", ".mp", ".mq", ".mr", ".ms", ".mt", ".mu", ".mv",
        ".mw", ".mx", ".my", ".mz", ".na", ".nc", ".ne", ".nf", ".ng", ".ni", ".nl", ".no",
        ".np", ".nr", ".nu", ".nz", ".om", ".pa", ".pe", ".pf", ".pg", ".ph", ".pk", ".pl",
        ".pm", ".pn", ".pr", ".ps", ".pt", ".pw", ".py", ".qa", ".re", ".ro", ".rs", ".ru",
        ".rw", ".sa", ".sb", ".sc", ".sd", ".se", ".sg", ".sh", ".si", ".sj", ".sk", ".sl",
        ".sm", ".sn", ".so", ".sr", ".ss", ".st", ".su", ".sv", ".sx", ".sy", ".sz", ".tc",
        ".td", ".tf", ".tg", ".th", ".tj", ".tk", ".tl", ".tm", ".tn", ".to", ".tr", ".tt",
        ".tv", ".tw", ".tz", ".ua", ".ug", ".uk", ".us", ".uy", ".uz", ".va", ".vc", ".ve",
        ".vg", ".vi", ".vn", ".vu", ".wf", ".ws", ".ye", ".yt", ".za", ".zm", ".zw",
        
        // Popular second-level domains
        ".com.au", ".com.br", ".com.cn", ".com.fr", ".com.de", ".com.in", ".com.jp",
        ".com.mx", ".com.ru", ".com.uk", ".gov.au", ".gov.br", ".gov.cn", ".gov.fr",
        ".gov.de", ".gov.in", ".gov.jp", ".gov.mx", ".gov.ru", ".gov.uk",
        
        // Additional country-specific domains
        ".co.uk", ".co.in", ".co.jp", ".co.za", ".co.nz", ".co.il", ".co.kr",
        
        // Regional domains
        ".eu", ".asia", ".africa",
        
        // Specialized domains
        ".edu", ".gov", ".mil"
    )
}