const Shimmer = () => (
    <div className="space-y-4 animate-pulse">
      {[...Array(3)].map((_, i) => (
        <div
          key={i}
          className="h-24 rounded-md bg-slate-300/60 shadow-inner"
        />
      ))}
    </div>
);

export default Shimmer;
