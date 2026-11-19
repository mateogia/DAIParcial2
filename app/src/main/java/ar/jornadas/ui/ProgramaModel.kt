package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class ProgramaModel(app: Application, private val estado: SavedStateHandle) : AndroidViewModel(app) {
    private val servicios = Servicios(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Filtros: sobreviven a rotación, navegación y muerte del proceso (Bundle de la tarea).
    val dia: StateFlow<Int> = estado.getStateFlow(CLAVE_DIA, 0)
    val tema: StateFlow<String> = estado.getStateFlow(CLAVE_TEMA, "Todos")
    fun elegirDia(valor: Int) { estado[CLAVE_DIA] = valor }
    fun elegirTema(valor: String) { estado[CLAVE_TEMA] = valor }

    val aviso = MutableStateFlow("")
    val cargando = MutableStateFlow(false)
    val hayEdicion = MutableStateFlow(false)   // existe una edición válida (puede estar vacía)
    val sinCopia = MutableStateFlow(false)     // falló y no hay nada guardado: ofrecer reintento

    private var cargaInicialHecha = false
    private var peticion: Job? = null

    // Una sola carga automática por instancia del ViewModel (= por sesión de proceso).
    fun cargaInicial() {
        if (cargaInicialHecha) return
        cargaInicialHecha = true
        recargar()
    }

    fun recargar() {
        if (peticion?.isActive == true) return   // ya hay una petición en curso
        peticion = viewModelScope.launch {
            cargando.value = true
            aviso.value = ""
            sinCopia.value = false
            try {
                repo.sincronizar()
                hayEdicion.value = true
            } catch (e: IOException) {
                informarFalla()
            } catch (e: HttpException) {
                informarFalla()
            } finally {
                cargando.value = false
            }
        }
    }

    private suspend fun informarFalla() {
        val guardada = repo.hayEdicionGuardada()
        hayEdicion.value = guardada
        if (guardada) {
            aviso.value = "Sin conexión: se están usando los datos guardados"
        } else {
            aviso.value = "No se pudo descargar el programa"
            sinCopia.value = true
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }

    private companion object {
        const val CLAVE_DIA = "filtro_dia"
        const val CLAVE_TEMA = "filtro_tema"
    }
}
