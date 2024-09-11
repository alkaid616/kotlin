// LATEST_LV_DIFFERENCE
package foo

import java.util.*

fun main()
{
  val c = ArrayList<Int>()
  c.add(3)
  System.out.println(++<!PARENTHESIZED_LHS_WARNING!>(c[0])<!>)
  System.out.println(<!PARENTHESIZED_LHS_WARNING!>(c[1])<!>--)
  System.out.println(-(c[2]))
}