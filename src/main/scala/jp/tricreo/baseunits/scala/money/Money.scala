package jp.tricreo.baseunits.scala.money

import java.util.{Locale, Currency}
import jp.tricreo.baseunits.scala.util.Ratio
import jp.tricreo.baseunits.scala.time.Duration


/**金額を表すクラス。
 * <p>ある一定の「量」と「通貨単位」から成るクラスである。</p>
 */
@serializable
class Money
(val amount: BigDecimal, val currency: Currency)
  extends Ordered[Money] {

  if (amount.scale != currency.getDefaultFractionDigits) {
    throw new IllegalArgumentException("Scale of amount does not match currency");
  }

  override def equals(obj: Any) = obj match {
    case that: Money => amount == that.amount && currency == that.currency
    //    case bd: BigDecimal => amount == bd
    //    case n: Int => amount == n
    //    case f: Float => amount == f
    //    case d: Double => amount == d
    case _ => false
  }

  override def hashCode = amount.hashCode + currency.hashCode

  /**Returns a [[Money]] whose amount is the absolute amount of this [[Money]], and whose scale is this.scale().
   *
   * @return 絶対金額
   */
  def abs = Money(amount.abs, currency)

  /**
   * 金額同士の比較を行う。
   *
   * <p>相対的に量が小さい方を「小さい」と判断する。通貨単位が異なる場合は {@link ClassCastException}を
   * スローするが、どちらか片方の量が{@code 0}である場合は例外をスローしない。</p>
   *
   * <p>例えば{@code 10 USD}と{@code 0 JPY}は、後者の方が小さい。
   * また、{@code 0 USD}と{@code 0 JPY}は同じである。</p>
   *
   * @param that 比較対象
   * @return {@link Comparable#compareTo(Object)}に準じる
   * @throws ClassCastException 比較対象の通貨単位が異なり、かつ双方の量がどちらも0ではない場合
   * @throws NullPointerException 引数に{@code null}を与えた場合
   */
  def compare(that: Money) = {
    require(currency == that.currency)
    amount compare that.amount
  }


  def /(divisor: Double) = dividedBy(divisor)

  def *(other: BigDecimal) = times(other)

  def +(other: Money) = {
    require(currency == other.currency)
    plus(other)
  }

  def -(other: Money) = {
    require(currency == other.currency)
    minus(other)
  }

  /**
   * この金額に対して、指定した{@code ratio}の割合の金額を返す。
   *
   * @param ratio 割合
   * @param scale スケール
   * @param roundingMode 丸めモード
   * @return 指定した割合の金額
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def applying(ratio: Ratio, scale: Int, roundingMode: BigDecimal.RoundingMode.Value): Money = {
    val newAmount = ratio.times(amount).decimalValue(scale, roundingMode)
    Money.adjustBy(newAmount, currency)
  }

  /**
   * この金額に対して、指定した{@code ratio}の割合の金額を返す。
   *
   * @param ratio 割合
   * @param roundingMode 丸めモード
   * @return 指定した割合の金額
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def applying(ratio: Ratio, roundingMode: BigDecimal.RoundingMode.Value): Money = {
    applying(ratio, currency.getDefaultFractionDigits, roundingMode);
  }

  /**
   * このオブジェクトの{@link #amount}フィールド（量）を返す。
   *
   * <p>CAUTION: このメソッドは、このオブジェクトがカプセル化する要素を外部に暴露する。取り扱いには充分注意のこと。</p>
   *
   * <p>How best to handle access to the internals? It is needed for
   * database mapping, UI presentation, and perhaps a few other
   * uses. Yet giving public access invites people to do the
   * real work of the Money object elsewhere.
   * Here is an experimental approach, giving access with a
   * warning label of sorts. Let us know how you like it.</p>
   *
   * @return 量
   */
  def breachEncapsulationOfAmount = amount

  /**
   * このオブジェクトの{@link #currency}フィールド（通貨単位）を返す。
   *
   * <p>CAUTION: このメソッドは、このオブジェクトがカプセル化する要素を外部に暴露する。取り扱いには充分注意のこと。</p>
   *
   * @return 通貨単位
   */
  def breachEncapsulationOfCurrency = currency

  /**
   * この金額を、{@code divisor}個に均等に分割した場合の金額を返す。
   *
   * <p>丸めモードは {@link RoundingMode#HALF_EVEN} を適用する。</p>
   *
   * @param divisor 除数
   * @return 金額
   */
  def dividedBy(divisor: Double): Money = {
    dividedBy(divisor, Money.DEFAULT_ROUNDING_MODE);
  }

  /**
   * この金額を、{@code divisor}個に均等に分割した場合の金額を返す。
   *
   * @param divisor 除数
   * @param roundingMode 丸めモード
   * @return 金額
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def dividedBy(divisor: BigDecimal, roundingMode: BigDecimal.RoundingMode.Value): Money = {
    val newAmount = amount.bigDecimal.divide(divisor.bigDecimal, roundingMode.id)
    Money(BigDecimal(newAmount), currency)
  }

  /**
   * この金額を、{@code divisor}個に均等に分割した場合の金額を返す。
   *
   * @param divisor 除数
   * @param roundingMode 丸めモード
   * @return 金額
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def dividedBy(divisor: Double, roundingMode: BigDecimal.RoundingMode.Value): Money = {
    dividedBy(BigDecimal(divisor), roundingMode)
  }

  /**
   * この金額の、{@code divisor}に対する割合を返す。
   *
   * @param divisor 除数
   * @return 割合
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   * @throws ClassCastException 引数の通貨単位がこのインスタンスの通貨単位と異なる場合
   * @throws ArithmeticException 引数{@code divisor}の量が0だった場合
   */
  def dividedBy(divisor: Money): Ratio = {
    checkHasSameCurrencyAs(divisor)
    Ratio(amount, divisor.amount)
  }

  /**
   * このインスタンがあらわす金額が、{@code other}よりも大きいかどうか調べる。
   *
   * <p>等価の場合は{@code false}とする。</p>
   *
   * @param other 基準金額
   * @return 大きい場合は{@code true}、そうでない場合は{@code false}
   * @throws ClassCastException 引数の通貨単位がこのインスタンスの通貨単位と異なる場合
   * @throws NullPointerException 引数に{@code null}を与えた場合
   */
  def isGreaterThan(other: Money) =
    this > other

  /**
   * このインスタンがあらわす金額が、{@code other}よりも小さいかどうか調べる。
   *
   * <p>等価の場合は{@code false}とする。</p>
   *
   * @param other 基準金額
   * @return 小さい場合は{@code true}、そうでない場合は{@code false}
   * @throws ClassCastException 引数の通貨単位がこのインスタンスの通貨単位と異なる場合
   * @throws NullPointerException 引数に{@code null}を与えた場合
   */
  def isLessThan(other: Money) =
    this < other

  /**
   * このインスタンがあらわす金額が、負の金額かどうか調べる。
   *
   * <p>ゼロの場合は{@code false}とする。</p>
   *
   * @return 負の金額である場合は{@code true}、そうでない場合は{@code false}
   */
  def isNegative =
    amount < BigDecimal(0)

  /**
   * このインスタンがあらわす金額が、正の金額かどうか調べる。
   *
   * <p>ゼロの場合は{@code false}とする。</p>
   *
   * @return 正の金額である場合は{@code true}、そうでない場合は{@code false}
   */
  def isPositive =
    amount > BigDecimal(0)

  /**
   * このインスタンがあらわす金額が、ゼロかどうか調べる。
   *
   * @return ゼロである場合は{@code true}、そうでない場合は{@code false}
   */
  def isZero = {
    equals(Money.adjustBy(0.0, currency))
  }

  /**
   * この金額から{@code other}を差し引いた金額を返す。
   *
   * @param other 金額
   * @return 差し引き金額
   * @throws ClassCastException 引数の通貨単位がこのインスタンスの通貨単位と異なる場合
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def minus(other: Money) = {
    plus(other.negated)
  }

  /**
   * Returns a {@link Money} whose amount is (-amount), and whose scale is this.scale().
   *
   * @return 金額
   */
  def negated =
    Money(BigDecimal(amount.bigDecimal.negate), currency);


  /**
   * 指定した時間量に対する、この金額の割合を返す。
   *
   * @param duration 時間量
   * @return 割合
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def per(duration: Duration): MoneyTimeRate =
    new MoneyTimeRate(this, duration)

  /**
   * この金額に{@code other}を足した金額を返す。
   *
   * @param other 金額
   * @return 足した金額
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   * @throws ClassCastException 引数の通貨単位がこのインスタンスの通貨単位と異なる場合
   */
  def plus(other: Money): Money = {
    checkHasSameCurrencyAs(other)
    Money.adjustBy(amount + other.amount, currency)
  }

  /**
   * この金額に{@code factor}を掛けた金額を返す。
   *
   * <p>丸めモードは {@link RoundingMode#HALF_EVEN} を適用する。</p>
   *
   * TODO: Many apps require carrying extra precision in intermediate
   * calculations. The use of Ratio is a beginning, but need a comprehensive
   * solution. Currently, an invariant of Money is that the scale is the
   * currencies standard scale, but this will probably have to be suspended or
   * elaborated in intermediate calcs, or handled with defered calculations
   * like Ratio.
   *
   * @param factor 係数
   * @return 掛けた金額
   */
  def times(factor: BigDecimal): Money = {
    times(factor, Money.DEFAULT_ROUNDING_MODE);
  }

  /**
   * この金額に{@code factor}を掛けた金額を返す。
   *
   * TODO: BigDecimal.multiply() scale is sum of scales of two multiplied
   * numbers. So what is scale of times?
   *
   * @param factor 係数
   * @param roundingMode 丸めモード
   * @return 掛けた金額
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def times(factor: BigDecimal, roundingMode: BigDecimal.RoundingMode.Value): Money = {
    Money.adjustBy(amount * factor, currency, roundingMode)
  }

  /**
   * この金額に{@code amount}を掛けた金額を返す。
   *
   * <p>丸めモードは {@link RoundingMode#HALF_EVEN} を適用する。</p>
   *
   * @param amount 係数
   * @return 掛けた金額
   */
  def times(amount: Double): Money =
    times(BigDecimal(amount));

  /**
   * この金額に{@code amount}を掛けた金額を返す。
   *
   * @param amount 係数
   * @param roundingMode 丸めモード
   * @return 掛けた金額
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def times(amount: Double, roundingMode: BigDecimal.RoundingMode.Value): Money = {
    times(BigDecimal(amount), roundingMode)
  }

  /**
   * この金額に{@code amount}を掛けた金額を返す。
   *
   * <p>丸めモードは {@link RoundingMode#HALF_EVEN} を適用する。</p>
   *
   * @param amount 係数
   * @return 掛けた金額
   */
  def times(amount: Int): Money =
    times(BigDecimal(amount))

  override def toString = {
    currency.getSymbol() + " " + amount
  }

  /**
   * 指定したロケールにおける、単位つきの金額表現の文字列を返す。
   *
   * @param locale ロケール。{@code null}の場合は {@link Locale#getDefault()} を利用する。
   * @return 金額の文字列表現
   */
  def toString(locale: Locale) = {
    var _locale = locale
    if (_locale == null) {
      _locale = Locale.getDefault
    }
    currency.getSymbol(_locale) + " " + amount
  }

  //	BigDecimal getAmount() {
  //		return amount;
  //	}
  //
  //	Currency getCurrency() {
  //		return currency;
  //	}

  private[money] def hasSameCurrencyAs(arg: Money) =
    currency.equals(arg.currency) || arg.amount.equals(BigDecimal(0)) || amount.equals(BigDecimal(0));

  /**
   * この金額に、最小の単位金額を足した金額、つまりこの金額よりも1ステップ分大きな金額を返す。
   *
   * @return この金額よりも1ステップ分大きな金額
   */
  private[money] def incremented =
    plus(minimumIncrement)

  /**
   * 最小の単位金額を返す。
   *
   * <p>例えば、日本円は1円であり、US$は1セント（つまり0.01ドル）である。</p>
   *
   * This probably should be Currency responsibility. Even then, it may need
   * to be customized for specialty apps because there are other cases, where
   * the smallest increment is not the smallest unit.
   *
   * @return 最小の単位金額
   */
  private[money] def minimumIncrement = {
    val increment = BigDecimal(1).bigDecimal.movePointLeft(currency.getDefaultFractionDigits)
    Money(BigDecimal(increment), currency)
  }

  private def checkHasSameCurrencyAs(aMoney: Money) {
    if (hasSameCurrencyAs(aMoney) == false) {
      throw new ClassCastException(aMoney.toString() + " is not same currency as " + this.toString());
    }
  }

  //  TODO: Provide some currency-dependent formatting. Java 1.4 Currency doesn't do it.
  //  public String formatString() {
  //      return currency.formatString(amount());
  //  }
  //  public String localString() {
  //      return currency.getFormat().format(amount());
  //  }

}

object Money {

  //implicit def bigDecimalToMoney(amount: Int) = apply(amount)

  val USD = Currency.getInstance("USD")

  val EUR = Currency.getInstance("EUR")

  val JPY = Currency.getInstance("JPY")

  val DEFAULT_ROUNDING_MODE = BigDecimal.RoundingMode.HALF_EVEN

  def apply(amount: BigDecimal, currency: Currency) = new Money(amount, currency)

  def unappy(money: Money) = Some(money.amount, money.currency)


  /**{@code amount}で表す量のドルを表すインスタンスを返す。
   *
   * <p>This creation method is safe to use. It will adjust scale, but will not
   * round off the amount.</p>
   *
   * @param amount 量
   * @return {@code amount}で表す量のドルを表すインスタンス
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def dollars(amount: BigDecimal) = adjustBy(amount, USD);

  /**
   * {@code amount}で表す量のドルを表すインスタンスを返す。
   *
   * <p>WARNING: Because of the indefinite precision of double, this method must
   * round off the value.</p>
   *
   * @param amount 量
   * @return {@code amount}で表す量のドルを表すインスタンス
   */
  def dollars(amount: Double) = adjustBy(amount, USD)

  /**
   * This creation method is safe to use. It will adjust scale, but will not
   * round off the amount.
   * @param amount 量
   * @return {@code amount}で表す量のユーロを表すインスタンス
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def euros(amount: BigDecimal) = adjustBy(amount, EUR)

  /**
   * WARNING: Because of the indefinite precision of double, this method must
   * round off the value.
   * @param amount 量
   * @return {@code amount}で表す量のユーロを表すインスタンス
   */
  def euros(amount: Double) = adjustBy(amount, EUR)

  /**
   * {@link Collection}に含む全ての金額の合計金額を返す。
   *
   * <p>合計金額の通貨単位は、 {@code monies}の要素の（共通した）通貨単位となるが、
   * {@link Collection}が空の場合は、現在のデフォルトロケールにおける通貨単位で、量が0のインスタンスを返す。</p>
   *
   * @param monies 金額の集合
   * @return 合計金額
   * @throws ClassCastException 引数の通貨単位の中に通貨単位が異なるものを含む場合。
   * 				ただし、量が0の金額については通貨単位を考慮しないので例外は発生しない。
   * @throws IllegalArgumentException 引数またはその要素に{@code null}を与えた場合
   */
  def sum(monies: Iterable[Money]) = {
    if (monies.isEmpty) {
      Money.zero(Currency.getInstance(Locale.getDefault()));
    } else {
      val iterator = monies.iterator
      var sum = iterator.next
      while (iterator.hasNext) {
        val each = iterator.next
        sum = sum + each
      }
      sum
    }
  }

  /**
   * This creation method is safe to use. It will adjust scale, but will not
   * round off the amount.
   *
   * @param amount 量
   * @param currency 通貨単位
   * @return 金額
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def adjustBy(amount: BigDecimal, currency: Currency): Money = {
    adjustBy(amount, currency, DEFAULT_ROUNDING_MODE) //BigDecimal.RoundingMode.UNNECESSARY)
  }

  /**
   * For convenience, an amount can be rounded to create a Money.
   *
   * @param rawAmount 量
   * @param currency 通貨単位
   * @param roundingMode 丸めモード
   * @return 金額
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def adjustBy(rawAmount: BigDecimal, currency: Currency, roundingMode: BigDecimal.RoundingMode.Value): Money = {
    val amount = rawAmount.setScale(currency.getDefaultFractionDigits, roundingMode)
    new Money(amount, currency)
  }

  /**
   * WARNING: Because of the indefinite precision of double, this method must
   * round off the value.
   *
   * @param dblAmount 量
   * @param currency 通貨単位
   * @return 金額
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def adjustBy(dblAmount: Double, currency: Currency): Money = {
    adjustBy(dblAmount, currency, DEFAULT_ROUNDING_MODE)
  }


  /**
   * Because of the indefinite precision of double, this method must round off
   * the value. This method gives the client control of the rounding mode.
   *
   * @param dblAmount 量
   * @param currency 通貨単位
   * @param roundingMode 丸めモード
   * @return 金額
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def adjustRound(dblAmount: Double, currency: Currency, roundingMode: BigDecimal.RoundingMode.Value): Money = {
    val rawAmount = BigDecimal(dblAmount)
    adjustBy(rawAmount, currency, roundingMode)
  }

  /**
   * This creation method is safe to use. It will adjust scale, but will not
   * round off the amount.
   *
   * @param amount 量
   * @return {@code amount}で表す量の円を表すインスタンス
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def yens(amount: BigDecimal) = adjustBy(amount, JPY)

  /**
   * WARNING: Because of the indefinite precision of double, this method must
   * round off the value.
   *
   * @param amount 量
   * @return {@code amount}で表す量の円を表すインスタンス
   */
  def yens(amount: Double): Money = adjustBy(amount, JPY)

  /**
   * 指定した通貨単位を持つ、量が0の金額を返す。
   *
   * @param currency 通貨単位
   * @return 金額
   * @throws IllegalArgumentException 引数に{@code null}を与えた場合
   */
  def zero(currency: Currency) = adjustBy(0.0, currency)


}