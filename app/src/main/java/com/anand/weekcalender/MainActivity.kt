package com.anand.weekcalender

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.TypefaceSpan
import androidx.databinding.DataBindingUtil
import com.alamkanak.weekview.WeekViewEntity
import com.alamkanak.weekview.threetenabp.WeekViewPagingAdapterThreeTenAbp
import com.alamkanak.weekview.threetenabp.firstVisibleDateAsLocalDate
import com.alamkanak.weekview.threetenabp.lastVisibleDateAsLocalDate
import com.alamkanak.weekview.threetenabp.scrollToDate
import com.anand.weekcalender.calenderUtils.CalendarEntity
import com.anand.weekcalender.calenderUtils.showToast
import com.anand.weekcalender.databinding.ActivityMainBinding
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        AndroidThreeTen.init(this)

        binding.weekView.adapter = adapter

        binding.dateRangeTextView.text = buildDateRangeText(
                startDate = binding.weekView.firstVisibleDateAsLocalDate,
                endDate = binding.weekView.lastVisibleDateAsLocalDate

        )

        binding.leftNavigationButton.setOnClickListener {
            val firstDate = binding.weekView.firstVisibleDateAsLocalDate
            val newFirstDate = firstDate.minusDays(7)
            val newLastDate = firstDate.minusDays(0)

            binding.weekView.scrollToDate(newFirstDate)

            binding.dateRangeTextView.text = buildDateRangeText(

                    startDate = newFirstDate,
                    endDate = newLastDate
            )

        }

        binding.rightNavigationButton.setOnClickListener {
            val firstDate = binding.weekView.firstVisibleDateAsLocalDate
            val newFirstDate = firstDate.plusDays(7)
            val newLastDate = firstDate.plusDays(13)
            binding.weekView.scrollToDate(newFirstDate)


            binding.dateRangeTextView.text = buildDateRangeText(
                    endDate = newLastDate,
                    startDate = newFirstDate
            )

        }


    }

    private fun setupDateRange() {
        val now = Calendar.getInstance()

        val min = now.clone() as Calendar
        min.set(Calendar.DAY_OF_MONTH, 1)
        binding.weekView.minDate = min

        val max = now.clone() as Calendar
        max.set(Calendar.DAY_OF_MONTH, max.getActualMaximum(Calendar.DAY_OF_MONTH))
        binding.weekView.maxDate = max
    }

    private val eventsFetcher: EventsFetcher by lazy { EventsFetcher(this) }


    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    private val adapter: StaticActivityWeekViewAdapter by lazy {
        StaticActivityWeekViewAdapter(

                loadMoreHandler = this::onLoadMore,
                rangeChangeHandler = this::onRangeChanged

        )
    }

    private fun onLoadMore(startDate: LocalDate, endDate: LocalDate) {
        eventsFetcher.fetch(startDate, endDate, adapter::submitList)
    }

    private fun onRangeChanged(startDate: LocalDate, endDate: LocalDate) {
//        dateRangeTextView.text = buildDateRangeText(startDate, endDate)
    }

    private fun buildDateRangeText(startDate: LocalDate, endDate: LocalDate): String {
        val formattedFirstDay = dateFormatter.format(startDate)
        val formattedLastDay = dateFormatter.format(endDate)
        return getString(R.string.date_infos, formattedFirstDay, formattedLastDay)
    }


    private class StaticActivityWeekViewAdapter(
            private val rangeChangeHandler: (startDate: LocalDate, endDate: LocalDate) -> Unit,
            private val loadMoreHandler: (startDate: LocalDate, endDate: LocalDate) -> Unit
    ) : WeekViewPagingAdapterThreeTenAbp<CalendarEntity.Event>() {

        private val formatter = DateTimeFormatter.ofLocalizedDateTime(
                FormatStyle.MEDIUM,
                FormatStyle.SHORT
        )

        override fun onCreateEntity(item: CalendarEntity.Event): WeekViewEntity {
            val backgroundColor = if (!item.isCanceled) item.color else Color.WHITE
            val textColor = if (!item.isCanceled) Color.WHITE else item.color
            val borderWidthResId =
                    if (!item.isCanceled) R.dimen.no_border_width else R.dimen.border_width

            val style = WeekViewEntity.Style.Builder()
                    .setTextColor(textColor)
                    .setBackgroundColor(backgroundColor)
                    .setBorderWidthResource(borderWidthResId)
                    .setBorderColor(item.color)
                    .build()

            val title = SpannableStringBuilder(item.title).apply {
                val titleSpan = TypefaceSpan("sans-serif-medium")
                setSpan(titleSpan, 0, item.title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (item.isCanceled) {
                    setSpan(
                            StrikethroughSpan(),
                            0,
                            item.title.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            val subtitle = SpannableStringBuilder(item.location).apply {
                if (item.isCanceled) {
                    setSpan(
                            StrikethroughSpan(),
                            0,
                            item.location.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            return WeekViewEntity.Event.Builder(item)
                    .setId(item.id)
                    .setTitle(title)
                    .setStartTime(item.startTime)
                    .setEndTime(item.endTime)
                    .setSubtitle(subtitle)
                    .setAllDay(item.isAllDay)
                    .setStyle(style)
                    .build()
        }

        override fun onEventClick(data: CalendarEntity.Event) {
            context.showToast("Clicked ${data.title}")
        }

        override fun onEmptyViewClick(time: LocalDateTime) {
            context.showToast("Empty view clicked at ${formatter.format(time)}")
        }

        override fun onEventLongClick(data: CalendarEntity.Event) {
            context.showToast("Long-clicked ${data.title}")
        }

        override fun onEmptyViewLongClick(time: LocalDateTime) {
            context.showToast("Empty view long-clicked at ${formatter.format(time)}")
        }

        override fun onLoadMore(startDate: LocalDate, endDate: LocalDate) {
            loadMoreHandler(startDate, endDate)
        }

        override fun onRangeChanged(firstVisibleDate: LocalDate, lastVisibleDate: LocalDate) {
            rangeChangeHandler(firstVisibleDate, lastVisibleDate)
        }
    }


}