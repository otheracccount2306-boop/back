package co.edu.ucc.orientacion.dto.response;

import java.util.List;

/**
 * Página de resultados con metadatos de paginación. La numeración de páginas inicia en 1.
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 * @param <T> tipo de los elementos de la página
 * @param content elementos de la página actual
 * @param page número de página actual, iniciando en 1
 * @param size tamaño máximo de la página
 * @param totalElements total de elementos que cumplen el filtro
 * @param totalPages total de páginas disponibles
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /**
     * Construye una página calculando el total de páginas.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param <T> tipo de los elementos de la página
     * @param content elementos de la página actual
     * @param page número de página actual, iniciando en 1
     * @param size tamaño máximo de la página
     * @param totalElements total de elementos que cumplen el filtro
     * @return página con metadatos calculados
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
