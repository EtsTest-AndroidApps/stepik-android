package org.stepik.android.remote.wishlist

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import org.stepic.droid.di.qualifiers.WishlistScheduler
import org.stepik.android.data.wishlist.source.WishlistRemoteDataSource
import org.stepik.android.domain.wishlist.model.WishlistEntry
import org.stepik.android.remote.wishlist.model.WishlistRequest
import org.stepik.android.remote.wishlist.model.WishlistResponse
import javax.inject.Inject

class WishlistRemoteDataSourceImpl
@Inject
constructor(
    private val wishlistService: WishlistService,
    @WishlistScheduler
    private val scheduler: Scheduler
) : WishlistRemoteDataSource {

    override fun getWishlistEntries(): Single<List<WishlistEntry>> =
        getWishlistEntriesByPage()
            .map { it.sortedByDescending(WishlistEntry::createDate) }
            .subscribeOn(scheduler)

    override fun saveWishlistEntry(courseId: Long): Single<WishlistEntry> =
        wishlistService
            .updateWishlist(WishlistRequest(courseId))
            .map { wishlistResponse -> wishlistResponse.wishlistEntries.first() }

    override fun removeWishlistEntry(wishlistEntryId: Long): Completable =
        wishlistService.removeWishlistEntry(wishlistEntryId)

    private fun getWishlistEntriesByPage(): Single<List<WishlistEntry>> =
        Observable.range(1, Integer.MAX_VALUE)
            .concatMapSingle { wishlistService.getWishlist(it) }
            .takeUntil { !it.meta.hasNext }
            .map(WishlistResponse::wishlistEntries)
            .reduce(emptyList()) { a, b -> a + b }
}