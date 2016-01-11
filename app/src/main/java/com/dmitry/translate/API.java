package com.dmitry.translate;

import java.util.Map;

import retrofit.Call;
import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;

public interface API {
    @FormUrlEncoded
    @POST("/api/v1.5/tr.json/translate")
    Call<Object> translate(@FieldMap Map<String,String> map);
/*        Можно изменять путь к файлу динамически:
        @GET("/{version}/users")
        Response getUsers(@Path("version") String version);
        Retrofit заменит слово “{version}” на, то которое вы передали методу.
        Сам аргумент должен быть аннотирован словом Path и в скобках нужно указать ключевое слово.*/
}
