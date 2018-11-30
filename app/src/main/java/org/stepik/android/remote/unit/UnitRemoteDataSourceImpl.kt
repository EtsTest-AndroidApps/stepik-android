package org.stepik.android.remote.unit

import io.reactivex.Maybe
import io.reactivex.Single
import org.stepic.droid.util.maybeFirst
import org.stepic.droid.web.Api
import org.stepic.droid.web.UnitMetaResponse
import org.stepik.android.data.unit.source.UnitRemoteDataSource
import org.stepik.android.model.Unit
import javax.inject.Inject

class UnitRemoteDataSourceImpl
@Inject
constructor(
    private val api: Api
) : UnitRemoteDataSource {
    override fun getUnit(unitId: Long): Maybe<Unit> =
        getUnits(unitId)
            .maybeFirst()

    override fun getUnits(vararg unitIds: Long): Single<List<Unit>> =
        api.getUnitsRx(unitIds)
            .map(UnitMetaResponse::units)
}