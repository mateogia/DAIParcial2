package ar.jornadas.data

import androidx.room.*
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

@Dao
abstract class CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    abstract fun observar(): Flow<List<Charla>>

    @Query("SELECT COUNT(*) FROM charlas")
    abstract suspend fun contar(): Int

    // Solo inserta charlas nuevas (con favorita = false); las existentes se ignoran.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarNuevas(fichas: List<Charla>)

    // Actualiza los datos del programa sin tocar la columna favorita.
    @Query("UPDATE charlas SET titulo = :titulo, tema = :tema, dia = :dia, hora = :hora, " +
           "sala = :sala, ponente = :ponente, resumen = :resumen WHERE id = :id")
    abstract suspend fun actualizarDatos(id: String, titulo: String, tema: String, dia: Int,
        hora: String, sala: String, ponente: String, resumen: String)

    @Query("DELETE FROM charlas WHERE id NOT IN (:vigentes)")
    abstract suspend fun retirarAusentes(vigentes: List<String>)

    @Query("DELETE FROM charlas")
    abstract suspend fun retirarTodas()

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    abstract suspend fun alternar(id: String)

    // Aplica una instantánea completa del programa de forma atómica.
    @Transaction
    open suspend fun aplicarEdicion(fichas: List<Charla>) {
        if (fichas.isEmpty()) retirarTodas() else retirarAusentes(fichas.map { it.id })
        insertarNuevas(fichas)
        fichas.forEach {
            actualizarDatos(it.id, it.titulo, it.tema, it.dia, it.hora, it.sala, it.ponente, it.resumen)
        }
    }
}

@Database(entities = [Charla::class], version = 1, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}
