using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading.Tasks;

namespace LRUCache
{
    internal class Node
    {
        internal object Key { get; }
        internal object Value { get; }

        internal Node Prev { get; set; }
        internal Node Next { get; set; }
        internal Action<object, object> EvictionCallback;

        internal Node(object key, object value)
        {
            Key = key;
            Value = value;
        }


        // No need to make these depend on prev/next pointers semantically.
        public override bool Equals(object? obj)
        {
            return obj is Node node &&
                   EqualityComparer<object>.Default.Equals(Key, node.Key) &&
                   EqualityComparer<object>.Default.Equals(Value, node.Value);
        }

        public override int GetHashCode()
        {
            return HashCode.Combine(Key, Value);
        }
    }
}
