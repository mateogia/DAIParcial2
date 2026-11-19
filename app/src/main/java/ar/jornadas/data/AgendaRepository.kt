package ar.jornadas.data

import android.content.SharedPreferences

class AgendaRepository(private val api: ProgramaApi, private val dao: CarteleraDao,
                       private val estado: SharedPreferences) {
    val programa = dao.observar()

    suspend fun sincronizar() {
        val fichas = api.consultar().map {
            Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
        }
        dao.aplicarEdicion(fichas)
        estado.edit().putBoolean(CLAVE_EDICION, true).apply()   // hubo una edición válida (aunque sea vacía)
    }

    // Marca explícita, o datos que dejó la versión anterior de la app.
    suspend fun hayEdicionGuardada(): Boolean =
        estado.getBoolean(CLAVE_EDICION, false) || dao.contar() > 0

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)

    private companion object { const val CLAVE_EDICION = "hayEdicion" }
}
