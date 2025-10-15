package com.dev.mymusicapp.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.dev.mymusicapp.model.Song;
import java.util.ArrayList;
import java.util.List;

/**
 * A classe SongRepository é responsável por obter os dados das músicas.
 * Ela abstrai a fonte de dados (neste caso, o MediaStore do Android) do resto da aplicação.
 */
public class SongRepository {

    /**
     * Busca todos os ficheiros de áudio do armazenamento externo do dispositivo.
     * @param context O contexto da aplicação, necessário para aceder ao ContentResolver.
     * @return Uma lista de objetos Song, cada um representando um ficheiro de música encontrado.
     */
    public List<Song> getSongs(Context context) {
        // Prepara uma lista vazia para armazenar as músicas encontradas.
        List<Song> songList = new ArrayList<>();

        // Obtém o ContentResolver, que é a ponte para aceder aos dados partilhados do Android (como músicas, contactos, etc.).
        ContentResolver contentResolver = context.getContentResolver();

        // Define o "endereço" (URI) da tabela que contém as informações sobre os ficheiros de áudio.
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        // Define quais "colunas" de informação queremos obter para cada música.
        String[] projection = {
                MediaStore.Audio.Media._ID,       // O ID único da música no MediaStore.
                MediaStore.Audio.Media.TITLE,     // O título da música.
                MediaStore.Audio.Media.ARTIST,    // O nome do artista.
                MediaStore.Audio.Media.DATA,      // O caminho completo do ficheiro no armazenamento.
                MediaStore.Audio.Media.DURATION,  // A duração da música em milissegundos.
                MediaStore.Audio.Media.ALBUM_ID   // O ID do álbum, usado para encontrar a capa.
        };

        // Cria um filtro para obter apenas ficheiros que são marcados como música (e não toques, alarmes, etc.).
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        // Executa a query (consulta) no sistema, que retorna um "Cursor" para percorrer os resultados.
        Cursor cursor = contentResolver.query(songUri, projection, selection, null, null);

        // Verifica se a consulta retornou algum resultado.
        if (cursor != null && cursor.moveToFirst()) {
            // Obtém o índice de cada coluna uma vez, para ser mais eficiente dentro do loop.
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            // Percorre cada linha (cada música) que o Cursor encontrou.
            do {
                // Extrai os dados de cada coluna para a música atual.
                long id = cursor.getLong(idColumn);
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                String dataPath = cursor.getString(dataColumn);
                long duration = cursor.getLong(durationColumn);
                long albumId = cursor.getLong(albumIdColumn);

                // Constrói o URI especial para a capa do álbum, usando o ID do álbum.
                Uri albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), albumId
                );

                // Cria um novo objeto Song com os dados extraídos e adiciona-o à nossa lista.
                songList.add(new Song(id, title, artist, dataPath, duration, albumArtUri.toString()));

            } while (cursor.moveToNext()); // Move para a próxima música encontrada.

            // Fecha o Cursor para libertar recursos do sistema. É um passo muito importante.
            cursor.close();
        }

        // Retorna a lista completa de músicas.
        return songList;
    }
}