import play.api.GlobalSettings
import play.api.mvc.Results._
import play.api.mvc._
import play.api.mvc.SimpleResult
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import Implicits.global

object AggregateRequestThrottling extends Filter {
  val outstandingRequests: AtomicInteger = new AtomicInteger(0)

  override def apply(next: RequestHeader => Future[SimpleResult])(request:RequestHeader) : Future[SimpleResult] = {

    println("Outstanding requests:"+outstandingRequests.get)
    try {
      outstandingRequests.incrementAndGet()
      val result = next(request)

      result.onComplete{case _ => outstandingRequests.decrementAndGet()}

      result.map(_ => Ok)
    } catch {
      case _: Throwable =>
        outstandingRequests.decrementAndGet()
        Future.successful[SimpleResult](ServiceUnavailable)
    }
  }
}

object Global extends WithFilters(AggregateRequestThrottling) with GlobalSettings {

}
