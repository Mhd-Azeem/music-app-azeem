package com.wavelength.music.di

import javax.inject.Qualifier

/** Distinguishes the Jamendo Retrofit/OkHttp stack from the JioSaavn one — they hit different
 * base URLs and only Jamendo needs the client_id/format query-param interceptor. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JamendoRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JioSaavnRetrofit
