package dev.journey.content

import dev.journey.domain.About
import dev.journey.domain.Author
import dev.journey.domain.Ending
import dev.journey.domain.Journey
import dev.journey.domain.Landmark
import dev.journey.domain.Source

/**
 * Hadrian's Wall Path, west-bound: Segedunum at Wallsend to Bowness-on-Solway, 135 km.
 *
 * Distances carry ±1–2 km. There is no published cumulative distance chart for the trail; these
 * are anchored on the six official route sections and interpolated by Wall mile. See
 * docs/research/hadrians-wall-landmarks.md, which also lists the factual traps this copy was
 * written against — the Sycamore Gap tree especially.
 *
 * The Robin Hood Inn is the one addition to the researched list. It sits inside the emptiest
 * 16 km of the walk, and its entry is about that emptiness rather than pretending otherwise.
 */
val HADRIANS_WALL = Journey(
    id = "hadrians-wall",
    name = "Hadrian's Wall",
    subtitle = "Wallsend to the Solway · 135 km",
    totalMetres = 135_000,

    author = Author(
        name = "Claude",
        bio = """
            An AI, writing from primary sources — English Heritage, Historic England, the Roman
            Inscriptions of Britain, and the trail's own custodians. Every entry cites what it drew
            on. Distances along this route are interpolated from the official route sections and
            carry a kilometre or two of error.
        """.trimIndent(),
    ),

    about = About(
        background = """
            Hadrian's Wall was begun around AD 122 and ran 73 miles from the Tyne to the Solway. It
            was not a battle line. It controlled movement: gates at every Roman mile, forts behind
            it, a customs and checkpoint system with an army attached. It was held for close to
            three hundred years, largely by auxiliary regiments raised elsewhere in the empire —
            Spain, Syria, Romania, the Low Countries, North Africa.

            It has never been the border between England and Scotland, and it never was in Roman
            times either. The modern border runs well north of it at the eastern end.

            The Hadrian's Wall Path is 84 miles — longer than the Wall — and opened in May 2003 as
            the fifteenth National Trail. It runs coast to coast and takes most walkers six or
            seven days.
        """.trimIndent(),
        whyThisOne = """
            It is a line with a beginning and an end, which most famous walks are not, and the
            things along it are unusually specific: a grain measure that holds more than it claims,
            a birthday invitation, a bath house found under a cricket pitch. It is also short
            enough to finish — about a month at an ordinary walking pace — which matters more than
            it sounds.
        """.trimIndent(),
        sources = listOf(
            Source("National Trails — Hadrian's Wall Path", "https://www.nationaltrail.co.uk/en_GB/trails/hadrians-wall-path/"),
            Source("English Heritage — Hadrian's Wall history", "https://www.english-heritage.org.uk/visit/places/hadrians-wall/history/"),
        ),
    ),

    ending = Ending(
        id = "hadrians-wall-ending",
        title = "The end of the Wall",
        body = """
            You have walked 135 km. The Wall itself was shorter — 73 miles against the path's 84 —
            and it was never the border between England and Scotland.

            It was held for roughly three centuries by regiments raised in Spain, Syria, Romania,
            the Low Countries and North Africa. Most of the men named on its altars and tombstones
            never saw Rome.

            At Bowness the ground runs out into the firth. That is where the frontier stopped.
        """.trimIndent(),
        sources = listOf(
            Source("English Heritage — Hadrian's Wall history", "https://www.english-heritage.org.uk/visit/places/hadrians-wall/history/"),
            Source("National Trails — Hadrian's Wall Path", "https://www.nationaltrail.co.uk/en_GB/trails/hadrians-wall-path/"),
        ),
    ),

    landmarks = listOf(

        Landmark(
            id = "segedunum",
            name = "Segedunum",
            metresFromStart = 0,
            standfirst = "The eastern end, where the frontier ran into the river",
            body = """
                The Wall did not originally reach Wallsend. It ended at Newcastle; this fort and a
                four-mile extension were added around AD 125–127, built to a narrower gauge than
                the original.

                A branch wall ran from the fort's south-east corner down into the Tyne, extended
                into the water to the low-tide mark. The garrison was around 600 men, infantry and
                cavalry together, on four acres.

                The fort was built over with terraced housing in the 1880s. After demolition,
                Charles Daniels excavated almost all of it between 1975 and 1984, making it the
                most fully investigated Roman fort in Britain.
            """.trimIndent(),
            sources = listOf(
                Source("North East Museums — Segedunum", "https://www.northeastmuseums.org.uk/segedunum/about-us"),
                Source("Current Archaeology — the Daniels excavations", "https://www.archaeology.co.uk/articles/review-segedunum-excavations-charles-daniels-roman-fort-wallsend-1975-1984.htm"),
            ),
        ),

        Landmark(
            id = "pons-aelius",
            name = "Pons Aelius",
            metresFromStart = 7_500,
            standfirst = "Two altars pulled out of the Tyne in 1875",
            body = """
                The Roman bridge here was called Pons Aelius. Aelius was Hadrian's family name, and
                it was the only bridge outside Rome named after an emperor.

                In 1875, men sinking the foundations of the Swing Bridge recovered two Roman altars
                from the north channel of the river. One is dedicated to Neptune and carved with a
                trident entwined by a dolphin; the other to Oceanus, carved with an anchor. They
                had stood in a shrine on the bridge.

                The fort's position was disputed until F. G. Simpson found it beneath the medieval
                castle in 1929. Its outline is now marked in stone beside the Castle Keep.
            """.trimIndent(),
            sources = listOf(
                Source("RIB 1319 — altar to Neptune", "https://romaninscriptionsofbritain.org/inscriptions/1319"),
                Source("RIB 1320 — altar to Oceanus", "https://romaninscriptionsofbritain.org/inscriptions/1320"),
                Source("Co-Curate — Pons Aelius", "https://co-curate.ncl.ac.uk/pons-aelius"),
            ),
        ),

        Landmark(
            id = "newburn",
            name = "Newburn Riverside",
            metresFromStart = 19_000,
            standfirst = "A brief battle in 1640 with disproportionate consequences",
            body = """
                No Wall along this stretch; the path follows the river.

                On 28 August 1640 a Scottish Covenanter army of around 20,000 under Alexander
                Leslie forded the Tyne here against roughly 4,500 English under Lord Conway. The
                English gun emplacements on the south bank were poorly sited and were destroyed by
                fire from the higher ground opposite.

                The Scots took Newcastle and with it London's coal supply. To buy them off, Charles
                I summoned the Long Parliament — the sequence that led to civil war two years
                later.
            """.trimIndent(),
            sources = listOf(
                Source("Battlefields Trust — Newburn Ford", "https://www.battlefieldstrust.com/resource-centre/battleview.asp?BattleFieldId=29"),
                Source("Historic England — Newburn Ford battlefield report", "https://historicengland.org.uk/content/docs/listing/battlefields/newburn-ford/"),
            ),
        ),

        Landmark(
            id = "heddon",
            name = "Heddon-on-the-Wall",
            metresFromStart = 24_000,
            standfirst = "The first upstanding Wall, built to the original broad gauge",
            body = """
                Around 220 metres of Wall survive here, standing about 1.4–1.7 m. It is the broad
                gauge — a specification of ten Roman feet, roughly 2.96 m — later reduced to eight.
                When Parker Brewis and F. G. Simpson measured it in 1926–27 they recorded 9 ft 7½
                in.

                English Heritage describes it as the longest and best preserved excavated stretch
                built to broad gauge, which is not the same as the longest stretch of Wall
                anywhere.

                A corn-drying kiln was later cut into the Wall's core at the west end: circular,
                1.9 m across, with a paved floor and flue. It was exposed some time between 1867
                and 1879.
            """.trimIndent(),
            sources = listOf(
                Source("English Heritage — Heddon-on-the-Wall", "https://www.english-heritage.org.uk/visit/places/heddon-on-the-wall-hadrians-wall/history/description/"),
                Source("Historic England — list entry 1010616", "https://historicengland.org.uk/listing/the-list/list-entry/1010616"),
            ),
        ),

        Landmark(
            id = "robin-hood-inn",
            name = "The Robin Hood Inn",
            metresFromStart = 35_000,
            standfirst = "The emptiest stretch, and the road that caused it",
            body = """
                Between 1751 and 1757 a military road was driven west from Heddon, largely along
                the Wall's foundations. For about fifteen miles the Wall was quarried into hardcore
                for it. That is why this section has almost nothing upstanding.

                It is usually called General Wade's Military Road. Wade died in 1748, three years
                before construction began.

                The inn is one of only seven official stamping stations for the Hadrian's Wall Path
                passport.
            """.trimIndent(),
            sources = listOf(
                Source("Per Lineam Valli — what is the Military Road?", "https://perlineamvalli.wordpress.com/2015/06/07/69-what-is-the-military-road/"),
                Source("National Trails — Hadrian's Wall Path", "https://www.nationaltrail.co.uk/en_GB/trails/hadrians-wall-path/"),
            ),
        ),

        Landmark(
            id = "portgate",
            name = "The Portgate",
            metresFromStart = 40_000,
            standfirst = "Where the road from York crossed the frontier",
            body = """
                Dere Street, the main road north from York, crossed the Wall here through a
                gatehouse roughly 11 m square. The site is now under a roundabout on the A68.

                Dorothy Charlesworth's 1966 excavation found the gate's west tower in the verge, a
                few inches north of the modern kerb. Nothing is visible today. The clearest
                surviving evidence is indirect: Milecastle 22, 400 m east, had its north gateway
                blocked with a metre-thick wall shortly after it was built, the new gate having
                made it redundant.

                Both names are post-Roman. "Portgate" is Old English; "Dere Street" comes from the
                Anglo-Saxon kingdom of Deira. The Roman names are lost.
            """.trimIndent(),
            sources = listOf(
                Source("Roman Britain — the Portgate", "https://www.roman-britain.co.uk/places/portgate/"),
                Source("RIB 1426 — FVLGVR DIVOM", "https://romaninscriptionsofbritain.org/inscriptions/1426"),
            ),
        ),

        Landmark(
            id = "chesters",
            name = "Chesters",
            metresFromStart = 49_000,
            standfirst = "A cavalry fort built deliberately across the Wall",
            body = """
                Built around AD 124 and set astride the Wall so that three of its gates open north
                of the frontier, allowing mounted patrols to ride out. From around AD 178 the
                garrison was the ala II Asturum, raised in northern Spain.

                The fort had 16 stable-barracks, each housing roughly 32 men and their horses. By
                the fourth century only 12 were in use. The regimental strongroom beneath the
                shrine still has its vaulted roof intact.

                The bath house by the North Tyne is the most complete set of Roman baths in
                Britain. The purpose of the seven arched niches in its changing hall is still
                disputed.
            """.trimIndent(),
            sources = listOf(
                Source("English Heritage — Chesters Roman Fort", "https://www.english-heritage.org.uk/visit/places/chesters-roman-fort-and-museum-hadrians-wall/history/description/"),
                Source("English Heritage — Brunton Turret", "https://www.english-heritage.org.uk/visit/places/brunton-turret-hadrians-wall/history/"),
            ),
        ),

        Landmark(
            id = "carrawburgh",
            name = "Carrawburgh",
            metresFromStart = 55_000,
            standfirst = "A temple found in a drought, and a spring full of coins",
            body = """
                The Temple of Mithras, built around AD 200, was found in the drought summer of 1949
                when the tops of three altars appeared through the turf. The altars standing there
                now are replicas; the originals, naming three successive commanding officers, are
                in the Great North Museum.

                Coventina's Well, found in 1876, was a basin 2.6 × 2.4 m built around a spring.
                Excavation recovered 13,487 coins — four gold, 184 silver, the rest bronze — along
                with ten altars and a relief of three water-nymphs.

                The fort sits on top of the filled-in Vallum, so it was added after the Wall system
                had already been laid out.
            """.trimIndent(),
            sources = listOf(
                Source("English Heritage — Temple of Mithras", "https://www.english-heritage.org.uk/visit/places/temple-of-mithras-carrawburgh-hadrians-wall/history/"),
                Source("Historic England — list entry 1015914", "https://historicengland.org.uk/listing/the-list/list-entry/1015914"),
            ),
        ),

        Landmark(
            id = "sewingshields",
            name = "Sewingshields Crags",
            metresFromStart = 62_000,
            standfirst = "Where the crags begin",
            body = """
                Milecastle 35 is unusual: its long axis runs parallel to the Wall rather than at
                right angles to it, as almost all others do. It measures about 18.3 × 15.2 m
                internally, with walls up to 3.2 m thick.

                Excavations in 1978–80 recovered eight spearheads, shield bosses, jet finger rings,
                gaming boards, six hand-mills and second-century window glass. Three medieval
                longhouses, dating between the mid-13th and early 15th centuries, were later built
                inside the milecastle.

                Local legend places King Arthur and his knights asleep in a cave beneath the crags.
            """.trimIndent(),
            sources = listOf(
                Source("English Heritage — Sewingshields Wall", "https://www.english-heritage.org.uk/visit/places/sewingshields-wall-hadrians-wall/history/"),
            ),
        ),

        Landmark(
            id = "housesteads",
            name = "Housesteads",
            metresFromStart = 64_000,
            standfirst = "The most complete Roman fort in Britain",
            body = """
                2.2 hectares, built within a decade of AD 122. The third-century garrison was the
                cohors I Tungrorum, 800 strong. Also attested is the numerus Hnaudifridi —
                "Notfried's own unit", a German irregular formation named after its commander.

                The latrines in the south-east corner are the best-preserved of any Roman fort in
                Britain: a room about 10 m long seating perhaps 20 men, flushed from a cistern
                holding close to 24,000 litres. It still runs when it rains.

                In 1932, beneath the clay floor of a shop in the civilian settlement, excavators
                found the skeletons of a man and a woman, with the broken tip of a sword lodged in
                the man's ribs. Burial inside a settlement was illegal under Roman law.
            """.trimIndent(),
            sources = listOf(
                Source("English Heritage — Housesteads Roman Fort", "https://www.english-heritage.org.uk/visit/places/housesteads-roman-fort-hadrians-wall/history/significance/"),
            ),
        ),

        Landmark(
            id = "sycamore-gap",
            name = "Sycamore Gap",
            metresFromStart = 67_000,
            standfirst = "The stump is alive, and the tree was younger than reported",
            body = """
                The sycamore was felled with a chainsaw during the night of 27–28 September 2023.
                Daniel Graham and Adam Carruthers were convicted of criminal damage in May 2025 and
                jailed for four years three months each. The damage was valued at £622,191 for the
                tree and £1,144 for the Wall, which the trunk struck as it fell.

                The stump is alive. The National Trust has recorded 25 shoots from the base,
                regrowing for a third year, and has not yet decided whether to keep it as a
                coppiced stool or reduce it to a single stem.

                Ring counts by Historic England put the tree at 100–120 years, not the 300 often
                reported. The largest surviving section of trunk stands upright indoors at The
                Sill, two miles away, where visitors are invited to touch it.
            """.trimIndent(),
            sources = listOf(
                Source("National Trust — next steps for the Sycamore Gap tree", "https://www.nationaltrust.org.uk/visit/north-east/hadrians-wall-and-housesteads-fort/next-steps-for-the-sycamore-gap-tree"),
                Source("Historic England — new research on the tree's age", "https://historicengland.org.uk/whats-new/news/new-research-age-sycamore-gap-tree/"),
                Source("CPS — pair who felled Sycamore Gap tree jailed", "https://cps.gov.uk/north-east/news/pair-who-felled-sycamore-gap-tree-jailed"),
                Source("The Sill — Sycamore Gap: Coming Home", "https://www.thesill.org.uk/sycamore-gap-coming-home/"),
            ),
        ),

        Landmark(
            id = "vindolanda",
            name = "Vindolanda",
            metresFromStart = 70_000,
            offRoute = true,
            standfirst = "Off the Wall, older than the Wall, and the richest site on the route",
            body = """
                Vindolanda is about 1.5 km south of the path and is not on Hadrian's Wall. The
                first fort here was built almost 40 years before the Wall, as part of the earlier
                Stanegate frontier.

                The waterlogged, oxygen-free soil preserves wood and leather. The 2017 season
                produced the only known Roman boxing gloves in existence, along with two cavalry
                swords and a pair of toy wooden swords.

                Around AD 97–105, Claudia Severa wrote to Sulpicia Lepidina inviting her to a
                birthday party on 11 September. Most of the tablet is in a scribe's hand; the
                closing lines are Severa's own, and are almost certainly the earliest surviving
                Latin written by a woman.
            """.trimIndent(),
            sources = listOf(
                Source("Vindolanda Trust — why is Vindolanda south of the Wall?", "https://www.vindolanda.com/faqs/why-is-vindolanda-to-the-south-of-hadrians-wall"),
                Source("RIB — Tab.Vindol. 291, Claudia Severa's invitation", "https://romaninscriptionsofbritain.org/inscriptions/TabVindol291"),
                Source("Current Archaeology — boxing gloves at Vindolanda", "https://archaeology.co.uk/articles/news/packing-a-punch-boxing-gloves-found-at-vindolanda.htm"),
            ),
        ),

        Landmark(
            id = "cawfields",
            name = "Cawfields",
            metresFromStart = 73_500,
            standfirst = "The cliff is a quarry, not a landscape",
            body = """
                The dramatic cliff face and flooded pool here are industrial. The Newcastle Granite
                & Whinstone Company began quarrying in 1902, destroying a long stretch of Wall
                before closing in 1952 under pressure from preservationists.

                Milecastle 42 survived. It was built by the Second Legion Augusta under the
                governor Aulus Platorius Nepos, and its gateways retain the circular pivot sockets
                for double inward-opening gates. Built into its fabric is a reused stone from the
                tombstone of Dagvalda, a soldier of the First Cohort of Pannonians, set up by his
                widow Pusinna.

                Great Chesters, just west, holds what is reputed to be the only Roman altar still
                standing in its original position anywhere on the Wall.
            """.trimIndent(),
            sources = listOf(
                Source("English Heritage — Cawfields Roman Wall", "https://www.english-heritage.org.uk/visit/places/cawfields-roman-wall-hadrians-wall/history/"),
                Source("Heritage Futures — the Great Chesters altar", "https://heritagefutures.wordpress.com/2017/02/24/great-chesters-roman-altar/"),
            ),
        ),

        Landmark(
            id = "walltown",
            name = "Walltown Crags",
            metresFromStart = 78_000,
            standfirst = "Syrian archers, and a grain measure that holds too much",
            body = """
                Nearly 400 m of Wall survive here, standing up to 2.2 m. Turret 45a predates the
                Wall: it began as a free-standing Stanegate signal tower and was later absorbed
                into the frontier, which is why the Wall meets it at odd angles with vertical
                straight joints.

                Carvoran, 250 yards south, was garrisoned by the cohors I Hamiorum sagittariorum —
                500 archers from Hama in Syria, the only regiment of archers known to have served
                in Britain.

                In June 1915 a postman found a bronze grain measure in a field here. Its
                inscription declares a capacity of 17½ sextarii. It actually holds about 20.8,
                which raises the question of whether the army was systematically over-measuring the
                grain tax.
            """.trimIndent(),
            sources = listOf(
                Source("English Heritage — Walltown Crags", "https://www.english-heritage.org.uk/visit/places/walltown-crags-hadrians-wall/history/"),
                Source("RIB — the Carvoran modius", "https://romaninscriptionsofbritain.org/instrumentum/2415"),
                Source("Roman Britain — Magnis (Carvoran)", "https://www.roman-britain.co.uk/places/magnis_carvetiorum/"),
            ),
        ),

        Landmark(
            id = "thirlwall",
            name = "Thirlwall Castle",
            metresFromStart = 81_000,
            standfirst = "A castle built out of the Wall, and the best evidence of its height",
            body = """
                Thirlwall Castle is built from dressed sandstone quarried by the Romans for
                Hadrian's Wall a few hundred metres away. John Thirlwall built or strengthened it
                in the 1330s. It was sold to the Earl of Carlisle in 1748 for £4,000 and left to
                decay.

                Poltross Burn Milecastle, just west, contains a surviving flight of stone steps
                whose angle indicates that the wall-walk stood about 3.66 m above ground, giving an
                external milecastle wall height of roughly 5.34 m. This is the best physical
                evidence for how tall the system stood.

                The milecastle is larger than standard, with an estimated garrison of 30. Its round
                oven was rebuilt five times.
            """.trimIndent(),
            sources = listOf(
                Source("Northumberland National Park — Thirlwall Castle", "https://www.northumberlandnationalpark.org.uk/discover-explore/places-to-visit/hadrians-wall/thirlwall-castle/"),
                Source("English Heritage — Poltross Burn Milecastle", "https://www.english-heritage.org.uk/visit/places/poltross-burn-milecastle-hadrians-wall/history/"),
            ),
        ),

        Landmark(
            id = "birdoswald",
            name = "Birdoswald",
            metresFromStart = 85_000,
            standfirst = "Where the end of Roman Britain is visible in the ground",
            body = """
                Banna, 2.14 hectares, begun shortly after AD 122. The garrison was the cohors I
                Aelia Dacorum, a 1,000-strong unit recruited in what is now Romania, whose
                inscriptions carry a curved Dacian sword as a badge.

                When the granaries went out of use, timber halls were built over the north granary
                in two successive phases, with occupation continuing unbroken into the fifth
                century. Tony Wilmott reads this as the fort becoming the base of a local warband
                descended from the late Roman garrison.

                At Willowford just east, the abutment of the bridge that carried the Wall over the
                Irthing now stands in a dry field: the river has since moved west. Walkers cross on
                a footbridge flown into place by helicopter in 1999.
            """.trimIndent(),
            sources = listOf(
                Source("English Heritage — Birdoswald Roman Fort", "https://www.english-heritage.org.uk/visit/places/birdoswald-roman-fort-hadrians-wall/history-and-stories/history/"),
                Source("English Heritage — Willowford Wall, turrets and bridge", "https://www.english-heritage.org.uk/visit/places/willowford-wall-turrets-and-bridge-hadrians-wall/history/"),
            ),
        ),

        Landmark(
            id = "pike-hill",
            name = "Pike Hill",
            metresFromStart = 89_000,
            standfirst = "A signal tower the Wall was later attached to, at 45 degrees",
            body = """
                The signal tower on Pike Hill predates the Wall. It was one of a series on high
                ground north of the Stanegate, signalling to Nether Denton and Castle Hill,
                Boothby. The Wall was later joined to it at about 45 degrees — the alignment is the
                tower's, not the Wall's, which is the visible proof of the sequence. Most of it was
                destroyed in 1870 when a road cutting was made beside it.

                Banks East Turret just east is the best-preserved turret in the western sector, and
                an anomaly: it was built of stone from the outset even though the Wall it served
                was turf.

                Frank Simpson's 1933 excavation found a mason's hammer, a coin of Hadrian, and the
                stone platform that supported the ladder to the upper floor. This was the first
                section of Hadrian's Wall taken into state guardianship.
            """.trimIndent(),
            sources = listOf(
                Source("English Heritage — Pike Hill Signal Tower", "https://www.english-heritage.org.uk/visit/places/pike-hill-signal-tower-hadrians-wall/history/"),
                Source("English Heritage — Banks East Turret", "https://www.english-heritage.org.uk/visit/places/banks-east-turret-hadrians-wall/history/"),
            ),
        ),

        Landmark(
            id = "walton",
            name = "Walton and the Turf Wall",
            metresFromStart = 95_000,
            standfirst = "West of the river, the Wall was first built out of turf",
            body = """
                From Harrow's Scar westward to Bowness, the Wall was first built in turf rather
                than stone — about 6 m wide at the base and probably 3.66 m high, turf-faced over
                an earth and clay core cut from the ground beside it.

                Turf Wall milecastles were turf and timber, but the turrets were stone from the
                start. The rebuild in stone came in two separate phases, decades apart.

                Hare Hill nearby carries the tallest surviving stretch of Wall, about 3 m — but its
                north face is a late-19th-century restoration, reset from fallen Roman facing
                stones collected by George Howard, 9th Earl of Carlisle.
            """.trimIndent(),
            sources = listOf(
                Source("English Heritage — Harrow's Scar Milecastle and Wall", "https://www.english-heritage.org.uk/visit/places/harrows-scar-milecastle-and-wall-hadrians-wall/history/"),
                Source("English Heritage — Hare Hill", "https://www.english-heritage.org.uk/visit/places/hare-hill-hadrians-wall/history/"),
            ),
        ),

        Landmark(
            id = "carlisle",
            name = "Carlisle",
            metresFromStart = 112_000,
            standfirst = "Fifty years older than the Wall, and still being excavated",
            body = """
                The first timber fort beneath Carlisle Castle is dated by dendrochronology on its
                rampart oaks to the autumn or winter of AD 72–73, built by a detachment of Legio IX
                Hispana — half a century before the Wall.

                Stanwix, now buried under a suburb, was the largest fort on the whole Wall at about
                177 × 213 m. Its garrison was the ala Petriana, 1,000 cavalry: the only
                thousand-strong cavalry regiment in Britain. Its commander outranked every other
                officer on the frontier.

                In 2017 an evaluation for a new cricket pavilion found a monumental bath house.
                Excavation has since recovered over 20,000 items, including 80 intaglios lost down
                the drains — the largest such group from any single context in the UK. The dig is
                still running.
            """.trimIndent(),
            sources = listOf(
                Source("Uncovering Roman Carlisle", "https://www.uncoveringromancarlisle.co.uk/learnmore"),
                Source("Historic England — Stanwix, list entry 1017948", "https://historicengland.org.uk/listing/the-list/list-entry/1017948"),
                Source("Roman Britain — Luguvalium", "https://www.roman-britain.co.uk/places/luguvalium/"),
            ),
        ),

        Landmark(
            id = "burgh-by-sands",
            name = "Burgh by Sands",
            metresFromStart = 122_000,
            standfirst = "Where Edward I died, and an African garrison is recorded",
            body = """
                Edward I died on Burgh Marsh on 7 July 1307, aged about 68, of dysentery, while
                waiting to cross the sands to fight Robert the Bruce. His body lay in state in St
                Michael's Church before being carried to Westminster.

                St Michael's is built inside the Roman fort and largely out of it. Its 14th-century
                west tower is a pele, entered through a narrow doorway defended by an iron yett —
                one of only three fortified church towers surviving in Cumbria.

                RIB 2042, an altar found in a cottage at Beaumont in 1934, was set up by the
                numerus Maurorum Aurelianorum, a unit of Moors raised in Mauretania, and dates to
                AD 253–8. The unit was still listed here a century later. It is the basis for the
                claim that this is the earliest recorded African community in Britain.
            """.trimIndent(),
            sources = listOf(
                Source("RIB 2042 — altar of the numerus Maurorum", "https://romaninscriptionsofbritain.org/inscriptions/2042"),
                Source("Cumbria County History Trust — first recorded African community", "https://www.cumbriacountyhistory.org.uk/first-recorded-african-community-britain-background-burgh-sands"),
                Source("Solway Coast — King Edward I monument", "https://www.solwaycoast-nl.org.uk/places-to-explore/historic-sites/king-edward-i-monument/"),
            ),
        ),

        Landmark(
            id = "bowness",
            name = "Bowness-on-Solway",
            metresFromStart = 135_000,
            standfirst = "The western end, and a dispute over church bells",
            body = """
                Maia was the second largest fort on the Wall at about 7½ acres, guarding the last
                fording point of the Solway estuary. The modern village sits inside its footprint,
                and St Michael's Church is built largely of stone taken from the fort.

                The church's bells, cast in 1611 and 1616, were stolen in a Scottish raid in 1626
                and dropped in the Solway. Bowness men raided back and returned with the bells of
                Dornock and Middlebie, which sit in the church porch to this day. Every new
                minister of Annan is still expected to ask formally for their return.

                The Banks, an Edwardian promenade looking over the firth, was designated the end of
                the Hadrian's Wall Path in 2002. Its shelter holds the final trail passport stamp.
            """.trimIndent(),
            sources = listOf(
                Source("Historic England — Bowness-on-Solway, list entry 1014702", "https://historicengland.org.uk/listing/the-list/list-entry/1014702"),
                Source("Visit Cumbria — Bowness-on-Solway", "https://www.visitcumbria.com/car/bowness-on-solway/"),
            ),
        ),
    ),
)
