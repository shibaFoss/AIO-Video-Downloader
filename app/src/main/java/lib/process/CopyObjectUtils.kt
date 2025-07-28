@file:Suppress("UNCHECKED_CAST")

package lib.process

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * Utility object for performing deep copies of serializable objects.
 *
 * Uses Java serialization to create a byte-level copy of the original object,
 * ensuring that the copy is entirely independent of the original.
 */
object CopyObjectUtils {
	
	/**
	 * Creates a deep copy of a serializable object using serialization.
	 *
	 * This method serializes the input object into a byte stream and then deserializes
	 * it back into a new instance, effectively cloning the object.
	 *
	 * @param T The type of the object, which must implement [Serializable].
	 * @param `object` The object to be copied.
	 * @return A deep copy of the object, or `null` if the operation fails.
	 */
	@JvmStatic
	fun <T : Serializable?> deepCopy(`object`: T): T? {
		return try {
			// Serialize the object to a byte array
			val bos = ByteArrayOutputStream()
			val oos = ObjectOutputStream(bos)
			oos.writeObject(`object`)
			oos.flush()
			oos.close()
			bos.close()
			
			// Deserialize the byte array back into an object
			val bis = ByteArrayInputStream(bos.toByteArray())
			val ois = ObjectInputStream(bis)
			val copy = ois.readObject() as T
			ois.close()
			bis.close()
			
			copy
		} catch (error: Exception) {
			// Print stack trace and return null on failure
			error.printStackTrace()
			null
		}
	}
}
