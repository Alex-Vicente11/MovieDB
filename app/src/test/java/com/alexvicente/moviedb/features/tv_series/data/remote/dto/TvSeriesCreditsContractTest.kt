package com.alexvicente.moviedb.features.tv_series.data.remote.dto

import com.alexvicente.moviedb.features.tv_series.data.mapper.TvSeriesMapper.toCast
import com.alexvicente.moviedb.testutil.Fixtures
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.junit.Test

/**
 * Contrato del reparto. En los fixtures el objeto "credits" va anidado dentro
 * de la respuesta de detalle (append_to_response), con la misma forma que
 * devuelve tv/{id}/credits: se extrae y se parsea como TvCreditsDto.
 */
class TvSeriesCreditsContractTest {

    private val gson = GsonBuilder().create()

    private fun creditsOf(fixtureName: String): TvCreditsDto {
        val root = JsonParser.parseString(Fixtures.read(fixtureName)).asJsonObject
        return gson.fromJson(root.get("credits"), TvCreditsDto::class.java)
    }

    @Test
    fun greysAnatomy_castParsesAndMapsToDomain() {
        val cast = creditsOf("tv_series_details_1416.json").toCast()

        assertThat(cast).hasSize(13)
        with(cast.first()) {
            assertThat(id).isEqualTo(9280)
            assertThat(creditId).isEqualTo("5256cf6919c2956ff609b6c8")
            assertThat(name).isEqualTo("Ellen Pompeo")
            assertThat(character).isEqualTo("Meredith Grey")
            assertThat(profilePath).isEqualTo("/yPd4G2RcjXQPNWkKUPbWh5v1flC.jpg")
        }
    }

    @Test
    fun chernobyl_castParses_andCrewIsIgnored() {
        val cast = creditsOf("tv_series_details_87108.json").toCast()

        assertThat(cast).hasSize(2)
        assertThat(cast.first().name).isEqualTo("Jared Harris")
        assertThat(cast.first().character).isEqualTo("Valery Legasov")
    }

    @Test
    fun sparseCast_mapsNullsToSafeDefaults() {
        val json = """
            {"cast": [{"id": 1, "credit_id": "c1", "name": "  Ana  ",
                       "character": null, "profile_path": null}]}
        """.trimIndent()

        val member = gson.fromJson(json, TvCreditsDto::class.java).toCast().single()

        assertThat(member.name).isEqualTo("Ana")
        assertThat(member.character).isEmpty()
        assertThat(member.profilePath).isNull()
    }

    @Test
    fun missingCastField_mapsToEmptyList() {
        assertThat(gson.fromJson("{}", TvCreditsDto::class.java).toCast()).isEmpty()
    }
}
