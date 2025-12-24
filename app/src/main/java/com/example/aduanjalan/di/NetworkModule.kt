package com.example.aduanjalan.di

import android.content.Context
import com.example.aduanjalan.data.remote.api.LaravelApiService
import com.example.aduanjalan.data.remote.api.OverpassApiService
import com.example.aduanjalan.ui.utils.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }

    @Provides
    fun provideLogging() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    @Provides
    fun provideOkHttp(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(logging).build()

    @Provides
    @Named("Laravel")
    fun provideLaravelRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
//            .baseUrl("http://192.168.43.48:8000/api/")
            .baseUrl("https://aduanjalanapi.bemftuho.id/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    fun provideLaravelApi(@Named("Laravel") retrofit: Retrofit): LaravelApiService =
        retrofit.create(LaravelApiService::class.java)

    @Provides
    @Singleton
    fun provideOverpassApiService(): OverpassApiService {
        return Retrofit.Builder()
            .baseUrl("https://overpass-api.de/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApiService::class.java)
    }

}
