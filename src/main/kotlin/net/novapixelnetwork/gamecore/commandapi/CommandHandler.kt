package net.novapixelnetwork.gamecore.commandapi

import net.novapixelnetwork.gamecore.messaging.Message
import net.novapixelnetwork.gamecore.messaging.Messages
import org.apache.commons.lang.StringUtils
import org.bukkit.entity.Player
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

class CommandHandler {


    internal fun execute(sender: Player, cmd: String, args: List<String>) {
        val section = CommandRegistry.getSection(cmd)!!
        for (set in CommandRegistry.getTree(section)) {
            if (StringUtils.join(args, " ").startsWith(set.key)) {
                val modifiedArgs: MutableList<String> = args.subList(if (StringUtils.split(set.key, ' ').isNotEmpty()) StringUtils.split(set.key, ' ').size else 0, args.size).toMutableList()
                if (modifiedArgs.size > 0) {
                    for (method in set.value::class.members) {
                        val hasAn = method.annotations.any { it.annotationClass == Command::class }

                        if (hasAn && method.name == modifiedArgs[0]) {
                            modifiedArgs.removeAt(0)
                            val toPass: MutableList<Any> = ArrayList()
                            toPass.addAll(modifiedArgs)
                            toPass.add(0, sender)
                            var x = 0
                            val newParams: MutableList<KParameter> = method.parameters.toMutableList()
                            newParams.removeAt(0)

                            for (param in newParams) {
                                if (toPass.size == x)
                                    break
                                if (x == 0) {
                                    x++
                                    continue
                                }
                                if (param.type != toPass[x]::class) {
                                    try {
                                        toPass[x] = ArgumentParser.INSTANCE.parse(toPass[x] as String, param.type.classifier as KClass<*>) as Any

                                    } catch(exc: ArgumentParseException) {
                                        sender.sendMessage(Messages.msg(Message.TYPE_ARGUMENT_CAST_ERROR, getParamName(param), exc.name))
                                        return
                                    } catch(exc: CommandException) {
                                        sender.sendMessage(exc.name)
                                        exc.printStackTrace()
                                        return
                                    }

                                }
                                x++
                            }
                            newParams.add(0, method.parameters[0])
                            if (method.parameters.size - 2 == modifiedArgs.size) {
                                method.call(set.value, *toPass.toTypedArray())
                                return
                            } else {
                                var params = ""
                                val requiredParams = method.parameters.toMutableList()
                                requiredParams.removeAt(0)
                                requiredParams.removeAt(0)
                                for (param in requiredParams) {
                                    params += "<${getParamName(param)}> "
                                }
                                sender.sendMessage(Messages.msg(Message.INVALID_USAGE, "/${set.key} ${method.name} $params"))
                                return
                            }

                        }
                    }
                } else {
                    section.onCommand(sender)
                }
                return
            }
            sender.sendMessage(Messages.msg(Message.UNKNOWN_COMMAND))
        }
    }

    private fun getParamName(p: KParameter): String {
        for (a in p.annotations) {
            println(a::class)
            println(Argument::class)
            if (a::class == Argument::class) {
                val arg = a as Argument
                return arg.name
            }
        }
        return p.name!!
    }

    companion object {
        val INSTANCE = CommandHandler()
    }

}