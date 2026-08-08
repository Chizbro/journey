package dev.journey.content

import dev.journey.domain.Journey
import dev.journey.domain.Landmark

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
    landmarks = listOf(

        Landmark(
            id = "segedunum",
            name = "Segedunum",
            metresFromStart = 0,
            standfirst = "Where the frontier walks into a river",
            body = """
                The frontier ends in water. From the fort's south-east corner a branch wall ran
                down into the Tyne and out to the low-tide mark — Rome drawing a line, then
                following it into the river. Six hundred men held this place, infantry and
                cavalry together, on four acres. The Wall did not originally come this far east;
                the fort and the four miles to it were added later, and built narrower, as though
                someone had looked at the map again. It vanished under terraced housing in the
                1880s and stayed buried for a century.
            """.trimIndent(),
        ),

        Landmark(
            id = "pons-aelius",
            name = "Pons Aelius",
            metresFromStart = 7_500,
            standfirst = "Two altars pulled out of the Tyne",
            body = """
                In 1875, men sinking the foundations of the Swing Bridge pulled two Roman altars
                out of the north channel of the river. One is dedicated to Neptune and carved with
                a trident entwined by a dolphin; the other to Oceanus, carved with an anchor. They
                had stood in a shrine on the Roman bridge, at the point where the river god gave
                way to the god of the tide. The bridge was Pons Aelius — Aelius was Hadrian's own
                family name, and it was the only bridge outside Rome named for an emperor.
            """.trimIndent(),
        ),

        Landmark(
            id = "newburn",
            name = "Newburn Riverside",
            metresFromStart = 19_000,
            standfirst = "A small fight with an enormous wake",
            body = """
                No Wall here; the path runs by the river. On 28 August 1640 a Scottish army of
                twenty thousand forded the Tyne at this spot against four and a half thousand
                English, whose gun emplacements were badly sited and were shot to pieces from the
                rising ground opposite. The fight was brief and the consequences were not. The
                Scots took Newcastle, and with it London's coal. To buy them off, Charles I had to
                summon the Parliament that would go on to make war on him.
            """.trimIndent(),
        ),

        Landmark(
            id = "heddon",
            name = "Heddon-on-the-Wall",
            metresFromStart = 24_000,
            standfirst = "The first real Wall, built to the old wide gauge",
            body = """
                The first Wall you meet, and it is the original broad specification — ten Roman
                feet, near enough. When it was measured in the 1920s it came out at nine feet
                seven and a half: the builders were working to a target, not a tolerance. Two
                hundred metres survive, standing chest-high. Someone later cut a corn-drying kiln
                into its core, circular and paved, and it went unnoticed until the 1870s. Ahead of
                you, the Military Road begins its dead-straight run west.
            """.trimIndent(),
        ),

        Landmark(
            id = "robin-hood-inn",
            name = "The Robin Hood Inn",
            metresFromStart = 35_000,
            standfirst = "The emptiest miles, and the reason for them",
            body = """
                This is the barest stretch of the whole walk, and the reason is under your feet.
                Between 1751 and 1757 a military road was driven west along the Wall's
                foundations, and for fifteen miles the Wall became hardcore. It is usually called
                General Wade's road, which is wrong — Wade died in 1748, three years before the
                first sod was cut. The inn here is one of only seven places on the trail that
                stamp a walker's passport. There is nothing Roman left to see. The absence is the
                monument.
            """.trimIndent(),
        ),

        Landmark(
            id = "portgate",
            name = "The Portgate",
            metresFromStart = 40_000,
            standfirst = "The gate in the Wall, now under a roundabout",
            body = """
                The great north road from York crossed the frontier here, through a gatehouse
                perhaps eleven metres square. It is under a roundabout now. An excavation in 1966
                found its west tower in the verge, a few inches north of the kerb. The clearest
                evidence is second-hand: a milecastle four hundred metres east had its north
                gateway blocked with a metre of masonry not long after it was built — the new gate
                had made it pointless. In a field nearby someone dug up a slab reading only
                FVLGVR DIVOM. The lightning of the gods.
            """.trimIndent(),
        ),

        Landmark(
            id = "chesters",
            name = "Chesters",
            metresFromStart = 49_000,
            standfirst = "A cavalry fort facing the wrong way on purpose",
            body = """
                Built deliberately astride the Wall so that three of its gates open on the north
                side — a place for riding out, not sitting behind. Its Spanish regiment kept
                sixteen stable-barracks, each holding about thirty-two men and their horses under
                one roof. You can still walk down steps into the regimental strongroom; its
                vaulted ceiling is intact. The bath house by the river is the most complete in
                Britain, and nobody has satisfactorily explained the seven arched niches in its
                changing room.
            """.trimIndent(),
        ),

        Landmark(
            id = "carrawburgh",
            name = "Carrawburgh",
            metresFromStart = 55_000,
            standfirst = "A temple found in a drought, and a well full of coins",
            body = """
                In the drought summer of 1949 the tops of three altars came up through the turf,
                and a temple of Mithras was underneath them. The altars standing there now are
                copies; the originals name three successive commanding officers. Nearby, a spring
                had been walled into a basin barely two and a half metres across. When it was
                emptied in 1876 it gave up thirteen thousand four hundred and eighty-seven coins,
                ten altars, and a carving of three water-nymphs — four centuries of people paying
                a goddess for something.
            """.trimIndent(),
        ),

        Landmark(
            id = "sewingshields",
            name = "Sewingshields Crags",
            metresFromStart = 62_000,
            standfirst = "The crags begin, and the walking changes",
            body = """
                The milecastle here is an oddity, laid out lengthways along the Wall instead of at
                right angles to it as almost all the others are. Digs in the late 1970s turned up
                eight spearheads, gaming boards and jet finger rings; later someone built three
                longhouses inside the Roman walls and lived in them for two hundred years. Beneath
                the crags, the story goes, Arthur and his knights are asleep, to be woken by
                blowing a bugle and cutting a garter with a stone sword.
            """.trimIndent(),
        ),

        Landmark(
            id = "housesteads",
            name = "Housesteads",
            metresFromStart = 64_000,
            standfirst = "The most complete fort on the Wall",
            body = """
                The most famous thing in it is the toilets. A room ten metres long seated about
                twenty men at once, flushed from a cistern holding twenty-four thousand litres. It
                still runs when it rains. Among the units posted here was one recorded as
                Notfried's own — a German irregular band named after the man who led it. In 1932,
                under a clay floor in the village outside the walls, excavators found a man and a
                woman with the broken tip of a sword still in his ribs. Burial inside a settlement
                was illegal. Someone needed them gone.
            """.trimIndent(),
        ),

        Landmark(
            id = "sycamore-gap",
            name = "Sycamore Gap",
            metresFromStart = 67_000,
            standfirst = "Not a stump — a stool, and it is growing",
            body = """
                The tree was cut down with a chainsaw on the night of 27 September 2023, and two
                men were jailed for it. What stands in the gap now is not a stump but a stool:
                living, fenced, pushing out new shoots — twenty-five of them counted, returning
                each spring. Nobody has decided yet whether to let it become a thicket or bring it
                back to a single stem. It was never as old as people said; the rings put it at a
                hundred years or so. The largest piece of the trunk is indoors two miles away,
                upright as it grew, and you are allowed to touch it.
            """.trimIndent(),
        ),

        Landmark(
            id = "vindolanda",
            name = "Vindolanda",
            metresFromStart = 70_000,
            offRoute = true,
            standfirst = "A detour, and worth it — the earliest Latin written by a woman",
            body = """
                Vindolanda is not on the Wall and never was; it stood on an older frontier road,
                built almost forty years before Hadrian's men arrived. The waterlogged ground here
                keeps wood and leather. It has given up the only Roman boxing gloves known to
                exist, and a thin sheet of wood on which Claudia Severa invited Sulpicia Lepidina
                to her birthday party on the eleventh of September, somewhere around AD 100. A
                scribe wrote most of it. The closing lines are in her own hand — the earliest
                surviving Latin written by a woman.
            """.trimIndent(),
        ),

        Landmark(
            id = "cawfields",
            name = "Cawfields",
            metresFromStart = 73_500,
            standfirst = "The cliff is a quarry, and the altar is still standing",
            body = """
                The cliff face and the still green pool look ancient and are not: a whinstone
                quarry opened here in 1902 and took a long stretch of Wall with it before closing
                in 1952. The milecastle survived. Its gateposts still carry the round sockets the
                doors turned in, and built into its fabric is a stone cut from the tombstone of a
                soldier called Dagvalda, set up by his widow Pusinna and then quietly recycled.
                West of here stands what is said to be the only Roman altar still in its original
                place on the whole Wall. Walkers leave coins on it.
            """.trimIndent(),
        ),

        Landmark(
            id = "walltown",
            name = "Walltown Crags",
            metresFromStart = 78_000,
            standfirst = "Syrian archers, and a measure that lies",
            body = """
                Four hundred metres of Wall, standing higher than your head. One turret here is
                older than the Wall itself — a free-standing signal tower the frontier later
                swallowed, meeting it at awkward angles. The fort just south was held by five
                hundred archers from Hama in Syria, the only regiment of archers known to have
                served in Britain. In 1915 a postman found a bronze grain measure in a field
                nearby. It is inscribed with its capacity. It holds about three units more than it
                claims, which raises an awkward question about the grain tax.
            """.trimIndent(),
        ),

        Landmark(
            id = "thirlwall",
            name = "Thirlwall Castle",
            metresFromStart = 81_000,
            standfirst = "A castle made out of the Wall",
            body = """
                Neatly dressed Roman sandstone, carried a few hundred metres and stacked into a
                tower house in the 1330s. It was sold in 1748 for four thousand pounds and left to
                fall down. A black dwarf is supposed to guard a golden table at the bottom of its
                well. Just west, a milecastle holds the best clue to how tall all this stood: a
                flight of stone steps whose angle puts the wall-walk about three and a half metres
                up. Its bread oven was rebuilt five times.
            """.trimIndent(),
        ),

        Landmark(
            id = "birdoswald",
            name = "Birdoswald",
            metresFromStart = 85_000,
            standfirst = "Where Rome ends slowly",
            body = """
                When the granaries fell out of use, someone built timber halls on top of the north
                one — and then built them again. Occupation runs unbroken into the fifth century,
                and the best reading is that the fort became the seat of a local warband descended
                from its own garrison, still drawing authority from the dead. The Romanian
                regiment stationed here carved a curved sword on its inscriptions as a badge. East
                of the fort a Roman bridge abutment stands in a dry field: the river moved west
                without it.
            """.trimIndent(),
        ),

        Landmark(
            id = "pike-hill",
            name = "Pike Hill",
            metresFromStart = 89_000,
            standfirst = "The tower came first, and the angle proves it",
            body = """
                A signal tower stood on this hill before the Wall existed, talking to other towers
                a mile or two off. When the Wall arrived it was simply attached to the tower at
                forty-five degrees — the angle is the tower's, not the Wall's, and it is the
                visible proof of which came first. In 1870 a road cutting destroyed most of it.
                The turret just east is the best preserved in the western sector, built of stone
                even though the Wall it served was turf. A dig in 1933 found a mason's hammer, a
                coin of Hadrian, and the stone base of the ladder.
            """.trimIndent(),
        ),

        Landmark(
            id = "walton",
            name = "Walton and the Turf Wall",
            metresFromStart = 95_000,
            standfirst = "From here west, the Wall was built out of grass",
            body = """
                Beyond the river the Wall was first raised in turf — six metres thick at the base,
                cut from the ground beside it, and standing perhaps as high as the stone ever did.
                It was replaced in stone later, in two separate pushes decades apart. Almost none
                of it is left to see; the official route description for this stretch opens by
                admitting that the lack of masonry won't concern you by now. The tallest surviving
                piece of Wall anywhere is near here, and a Victorian earl rebuilt its north face
                from fallen stones he had collected.
            """.trimIndent(),
        ),

        Landmark(
            id = "carlisle",
            name = "Carlisle",
            metresFromStart = 112_000,
            standfirst = "Fifty years older than the Wall, and still being dug",
            body = """
                Tree rings in the first fort's ramparts date its timbers to the winter of AD 72 —
                half a century before Hadrian. The largest fort on the whole frontier is here too,
                buried under a suburb, and it held the only thousand-strong cavalry regiment in
                Britain; its commander outranked every other officer on the Wall. In 2017 an
                evaluation for a new cricket pavilion hit a bath house. Eighty engraved gemstones
                had washed down its drains, the largest group ever found in one place in this
                country. They are still digging.
            """.trimIndent(),
        ),

        Landmark(
            id = "burgh-by-sands",
            name = "Burgh by Sands",
            metresFromStart = 122_000,
            standfirst = "A king dies on the marsh; a garrison from North Africa",
            body = """
                Edward I died out on the marsh here on 7 July 1307, waiting to cross the sands and
                fight Robert the Bruce. He was carried into St Michael's to lie in state — a
                church built inside the Roman fort, out of the Roman fort, with a tower you enter
                through a narrow door behind an iron grille. An altar found in a cottage nearby
                was raised by a unit of Moors from North Africa stationed at this fort in the
                250s. They were still listed here a century later: the earliest recorded African
                community in Britain.
            """.trimIndent(),
        ),

        Landmark(
            id = "bowness",
            name = "Bowness-on-Solway",
            metresFromStart = 135_000,
            standfirst = "The end of the Wall, and the business of the bells",
            body = """
                The village sits inside the footprint of the second largest fort on the Wall,
                which guarded the last place the Solway could be forded. Its church is built out
                of that fort. In 1626 Scottish raiders stole the church bells and dropped them in
                the firth; Bowness men rowed over and came back with the bells of Dornock and
                Middlebie, which are in the porch to this day — and every new minister of Annan is
                still expected to ask politely for their return. At The Banks, a shelter looks out
                over the water. That is where the path stops.
            """.trimIndent(),
        ),
    ),
)
