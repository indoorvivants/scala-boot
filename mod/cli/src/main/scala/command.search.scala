package scalaboot

import scribe.Level
import scalaboot.client.Retries
import scalaboot.client.Client
import concurrent.duration.*

def commandSearch(config: CLI.Search) =
  if config.verbose then
    scribe.Logger.root.withMinimumLevel(Level.Debug).replace()

  val retries = Retries.exponential(5, 30.millis)
  val baseClient =
    Client.create(config.api.getOrElse(protocol.SCALABOOT_PRODUCTION))
  val client = Client.stabilise(
    baseClient,
    retries,
    (label, attempt) =>
      scribe.debug(
        s"[$label] failed, retrying in ${attempt.action.sleep}ms (${attempt.action.remaining} more attempts left)"
      )
  )

  val sorted = client.search(config.query).sortBy(_.rank).reverse
  val maxStars = sorted.maxByOption(_.repo.stars).map(_.repo.stars).getOrElse(0)
  val starsFieldLength = maxStars.toString().length
  val placeFieldLength = sorted.length.toString.length() + 2

  def renderStars(stars: Int, place: Int) =
    val spacer = " " * (starsFieldLength - stars.toString.length)
    s"⭐️ ${fansi.Bold.On(stars.toString)}$spacer"

  def renderPlace(place: Int) =
    val spacer = " " * (placeFieldLength - place.toString().length)
    s"[${place}]$spacer"

  val limit = if config.all then sorted.size else 5

  def repoName(nm: String) =
    nm match
      case s"$org/$repo" =>
        fansi.Str(org) ++ fansi.Color.DarkGray("/") ++
          fansi.Color.Green(repo)

  sorted.take(limit).zipWithIndex.foreach { case (result, idx) =>
    println(
      renderPlace(idx + 1) + renderStars(result.repo.stars, idx + 1) + "  " +
        fansi.Color
          .DarkGray("https://github.com/") + repoName(result.repo.name)
    )
  }

  if sorted.size > limit then
    println(
      s"Displaying only top 5 results, to see the remaining ${sorted.size - limit}, pass a ${fansi.Bold.On("-a")} flag"
    )

  if config.interactive then

    if config.yes then
      println(
        s"You've passed ${fansi.Bold.On("-y")} flag, so template will render with defaults automatically"
      )
    val defaultPrompt =
      "Please enter a number or press Enter to choose first one: "

    def go(prompt: String): Int =
      io.StdIn.readLine(prompt).trim() match
        case "" =>
          0
        case other =>
          other.toIntOption match
            case Some(num) if num >= 1 && num <= limit =>
              num - 1
            case _ =>
              go(defaultPrompt)

    val choice = go(defaultPrompt)

    val template = sorted(choice)

    val goConfig: CLI.Go =
      CLI.Go(
        template = template.repo.name,
        yes = config.yes,
        verbose = config.verbose
      )

    commandGo(goConfig)
  end if

end commandSearch
